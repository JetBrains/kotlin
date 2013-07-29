/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.java.provider;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.JetJavaMirrorMarker;
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaPackage;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiFieldWrapper;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMemberWrapper;
import org.jetbrains.jet.lang.resolve.java.wrapper.PsiMethodWrapper;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class MembersCache {
    private final Multimap<Name, Runnable> memberProcessingTasks = HashMultimap.create();
    private final Map<Name, NamedMembers> namedMembersMap = new HashMap<Name, NamedMembers>();

    @Nullable
    public NamedMembers get(@NotNull Name name) {
        runTasksByName(name);
        return namedMembersMap.get(name);
    }

    @NotNull
    public Collection<NamedMembers> allMembers() {
        runAllTasks();
        memberProcessingTasks.clear();
        return namedMembersMap.values();
    }

    @NotNull
    private NamedMembers getOrCreateEmpty(@NotNull Name name) {
        NamedMembers r = namedMembersMap.get(name);
        if (r == null) {
            r = new NamedMembers(name);
            namedMembersMap.put(name, r);
        }
        return r;
    }

    private void addTask(@NotNull PsiMember member, @NotNull RunOnce task) {
        addTask(member.getName(), task);
    }

    private void addTask(@Nullable String name, @NotNull RunOnce task) {
        if (name == null) {
            return;
        }
        memberProcessingTasks.put(Name.identifier(name), task);
    }

    private void runTasksByName(Name name) {
        if (!memberProcessingTasks.containsKey(name)) return;
        Collection<Runnable> tasks = memberProcessingTasks.get(name);
        for (Runnable task : tasks) {
            task.run();
        }
        // Delete tasks
        tasks.clear();
    }

    private void runAllTasks() {
        for (Runnable task : memberProcessingTasks.values()) {
            task.run();
        }
    }

    @NotNull
    public static MembersCache buildMembersByNameCache(
            @Nullable JavaClass javaClass,
            @Nullable JavaPackage javaPackage,
            boolean staticMembers
    ) {
        MembersCache membersCache = new MembersCache();

        if (javaClass != null) {
            membersCache.new ClassMemberProcessor(javaClass, staticMembers).process();
            Collection<JavaClass> innerClasses = javaClass.getInnerClasses();
            membersCache.new ExtraPackageMembersProcessor(innerClasses).process();
        }
        else if (javaPackage != null) {
            Collection<JavaClass> classes = DescriptorResolverUtils.filterDuplicateClasses(javaPackage.getClasses());
            membersCache.new ExtraPackageMembersProcessor(classes).process();
        }

        return membersCache;
    }

    private class ExtraPackageMembersProcessor { // 'extra' means that PSI elements for these members are not just top-level classes
        @NotNull
        private final Collection<JavaClass> javaClasses;

        private ExtraPackageMembersProcessor(@NotNull Collection<JavaClass> javaClasses) {
            this.javaClasses = javaClasses;
        }

        private void process() {
            for (JavaClass javaClass : javaClasses) {
                PsiClass psiClass = javaClass.getPsiClass();

                if (!(psiClass instanceof JetJavaMirrorMarker)) { // to filter out JetLightClasses
                    if (SingleAbstractMethodUtils.isSamInterface(psiClass)) {
                        processSamInterface(psiClass);
                    }
                }
            }
        }

        private void processSamInterface(@NotNull PsiClass psiClass) {
            NamedMembers namedMembers = getOrCreateEmpty(Name.identifier(psiClass.getName()));
            namedMembers.setSamInterface(psiClass);
        }
    }

    private class ClassMemberProcessor {
        @NotNull
        private final PsiClass psiClass;
        private final boolean staticMembers;

        private ClassMemberProcessor(@NotNull JavaClass javaClass, boolean staticMembers) {
            this.psiClass = javaClass.getPsiClass();
            this.staticMembers = staticMembers;
        }

        public void process() {
            processFields();
            processMethods();
            processNestedClasses();
        }

        private void processFields() {
            for (final PsiField field : psiClass.getAllFields()) {
                addTask(field, new RunOnce() {
                    @Override
                    public void doRun() {
                        processField(field);
                    }
                });
            }
        }

        private void processMethods() {
            createEntriesForAllMethods();
            processOwnMethods();
        }

        private void createEntriesForAllMethods() {
            for (PsiMethod method : psiClass.getAllMethods()) {
                getOrCreateEmpty(Name.identifier(method.getName()));
            }
        }

        private void processOwnMethods() {
            for (final PsiMethod method : psiClass.getMethods()) {
                addTask(method, new RunOnce() {
                    @Override
                    public void doRun() {
                        processOwnMethod(method);
                    }
                });
            }
        }

        private void processNestedClasses() {
            if (!staticMembers) {
                return;
            }
            for (final PsiClass nested : psiClass.getInnerClasses()) {
                addTask(nested, new RunOnce() {
                    @Override
                    public void doRun() {
                        processNestedClass(nested);
                    }
                });
            }
        }

        private boolean includeMember(PsiMemberWrapper member) {
            if (psiClass.isEnum() && staticMembers) {
                return member.isStatic();
            }

            if (member.isStatic() != staticMembers) {
                return false;
            }

            if (member.getPsiMember().getContainingClass() != psiClass) {
                return false;
            }

            if (member.isPrivate()) {
                return false;
            }

            if (DescriptorResolverUtils.isObjectMethodInInterface(member.getPsiMember())) {
                return false;
            }

            return true;
        }

        private void processField(PsiField field) {
            PsiFieldWrapper fieldWrapper = new PsiFieldWrapper(field);

            // group must be created even for excluded field
            NamedMembers namedMembers = getOrCreateEmpty(Name.identifier(fieldWrapper.getName()));

            if (!includeMember(fieldWrapper)) {
                return;
            }

            namedMembers.addField(fieldWrapper);
        }

        private void processOwnMethod(PsiMethod ownMethod) {
            PsiMethodWrapper method = new PsiMethodWrapper(ownMethod);

            if (!includeMember(method)) {
                return;
            }

            NamedMembers namedMembers = getOrCreateEmpty(Name.identifier(method.getName()));
            namedMembers.addMethod(method);
        }

        private void processNestedClass(PsiClass nested) {
            if (SingleAbstractMethodUtils.isSamInterface(nested)) {
                NamedMembers namedMembers = getOrCreateEmpty(Name.identifier(nested.getName()));
                namedMembers.setSamInterface(nested);
            }
        }
    }

    private static abstract class RunOnce implements Runnable {
        private boolean hasRun = false;

        @Override
        public final void run() {
            if (hasRun) return;
            hasRun = true;
            doRun();
        }

        protected abstract void doRun();
    }
}
