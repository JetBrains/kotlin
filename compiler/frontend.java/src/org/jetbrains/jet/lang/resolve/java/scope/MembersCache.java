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

package org.jetbrains.jet.lang.resolve.java.scope;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.jetAsJava.JetJavaMirrorMarker;
import org.jetbrains.jet.lang.resolve.java.sam.SingleAbstractMethodUtils;
import org.jetbrains.jet.lang.resolve.java.structure.*;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/* package */ final class MembersCache {
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
    private NamedMembers getOrCreateEmpty(@NotNull JavaNamedElement element) {
        Name name = element.getName();
        NamedMembers r = namedMembersMap.get(name);
        if (r == null) {
            r = new NamedMembers(name);
            namedMembersMap.put(name, r);
        }
        return r;
    }

    private void addTask(@NotNull JavaNamedElement member, @NotNull RunOnce task) {
        memberProcessingTasks.put(member.getName(), task);
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
    public static MembersCache buildForClass(@NotNull JavaClass javaClass, boolean staticMembers) {
        MembersCache membersCache = new MembersCache();

        membersCache.new ClassMemberProcessor(javaClass, staticMembers).process();
        membersCache.new ExtraPackageMembersProcessor(javaClass.getInnerClasses()).process();

        return membersCache;
    }

    @NotNull
    public static MembersCache buildForPackage(@NotNull JavaPackage javaPackage) {
        MembersCache membersCache = new MembersCache();

        Collection<JavaClass> classes = DescriptorResolverUtils.filterDuplicateClasses(javaPackage.getClasses());
        membersCache.new ExtraPackageMembersProcessor(classes).process();

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
                if (!(javaClass.getPsi() instanceof JetJavaMirrorMarker)) { // to filter out JetLightClasses
                    if (SingleAbstractMethodUtils.isSamInterface(javaClass)) {
                        processSamInterface(javaClass);
                    }
                }
            }
        }

        private void processSamInterface(@NotNull JavaClass javaClass) {
            getOrCreateEmpty(javaClass).setSamInterface(javaClass);
        }
    }

    private class ClassMemberProcessor {
        @NotNull
        private final JavaClass javaClass;
        private final boolean staticMembers;

        private ClassMemberProcessor(@NotNull JavaClass javaClass, boolean staticMembers) {
            this.javaClass = javaClass;
            this.staticMembers = staticMembers;
        }

        private void process() {
            processFields();
            processMethods();
            processNestedClasses();
        }

        private void processFields() {
            for (final JavaField field : javaClass.getAllFields()) {
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
            for (JavaMethod method : javaClass.getAllMethods()) {
                getOrCreateEmpty(method);
            }
        }

        private void processOwnMethods() {
            for (final JavaMethod method : javaClass.getMethods()) {
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
            for (final JavaClass nested : javaClass.getInnerClasses()) {
                addTask(nested, new RunOnce() {
                    @Override
                    public void doRun() {
                        processNestedClass(nested);
                    }
                });
            }
        }

        private boolean includeMember(@NotNull JavaMember member) {
            if (javaClass.isEnum() && staticMembers) {
                return member.isStatic();
            }

            if (member.isStatic() != staticMembers) {
                return false;
            }

            if (!isInCurrentClass(member)) {
                return false;
            }

            if (member.getVisibility() == Visibilities.PRIVATE) {
                return false;
            }

            if (DescriptorResolverUtils.isObjectMethodInInterface(member)) {
                return false;
            }

            return true;
        }

        private boolean isInCurrentClass(@NotNull JavaMember member) {
            FqName fqName = member.getContainingClass().getFqName();
            return fqName != null && fqName.equals(javaClass.getFqName());
        }

        private void processField(@NotNull JavaField field) {
            // group must be created even for excluded field
            NamedMembers namedMembers = getOrCreateEmpty(field);

            if (includeMember(field)) {
                namedMembers.addField(field);
            }
        }

        private void processOwnMethod(@NotNull JavaMethod ownMethod) {
            if (includeMember(ownMethod)) {
                getOrCreateEmpty(ownMethod).addMethod(ownMethod);
            }
        }

        private void processNestedClass(@NotNull JavaClass nested) {
            if (SingleAbstractMethodUtils.isSamInterface(nested)) {
                getOrCreateEmpty(nested).setSamInterface(nested);
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
