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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.intellij.psi.*;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JetJavaMirrorMarker;
import org.jetbrains.jet.lang.resolve.java.PsiClassFinder;
import org.jetbrains.jet.lang.resolve.java.TypeSource;
import org.jetbrains.jet.lang.resolve.java.prop.PropertyNameUtils;
import org.jetbrains.jet.lang.resolve.java.prop.PropertyParseResult;
import org.jetbrains.jet.lang.resolve.java.wrapper.*;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.psi.util.MethodSignatureUtil.areSignaturesErasureEqual;
import static com.intellij.psi.util.PsiFormatUtilBase.*;

public final class MembersCache {
    private static final ImmutableSet<String> OBJECT_METHODS = ImmutableSet.of("hashCode()", "equals(java.lang.Object)", "toString()");

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
            @NotNull MembersCache membersCache,
            @NotNull PsiClassFinder finder,
            @Nullable PsiClass psiClass,
            @Nullable PsiPackage psiPackage,
            boolean staticMembers
    ) {
        if (psiClass != null) {
            membersCache.new ClassMemberProcessor(new PsiClassWrapper(psiClass), staticMembers).process();
        }

        //TODO:
        List<PsiClass> classes = psiPackage != null ? finder.findPsiClasses(psiPackage) : finder.findInnerPsiClasses(psiClass);
        membersCache.new ExtraPackageMembersProcessor(classes).process();
        return membersCache;
    }

    private class ExtraPackageMembersProcessor { // 'extra' means that PSI elements for these members are not just top-level classes
        @NotNull
        private final List<PsiClass> psiClasses;

        private ExtraPackageMembersProcessor(@NotNull List<PsiClass> classes) {
            psiClasses = classes;
        }

        private void process() {
            for (PsiClass psiClass : psiClasses) {
                if (!(psiClass instanceof JetJavaMirrorMarker)) { // to filter out JetLightClasses
                    if (isSamInterface(psiClass)) {
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
        private final PsiClassWrapper psiClass;
        private final boolean staticMembers;

        private ClassMemberProcessor(@NotNull PsiClassWrapper psiClass, boolean staticMembers) {
            this.psiClass = psiClass;
            this.staticMembers = staticMembers;
        }

        public void process() {
            processFields();
            processMethods();
            processNestedClasses();
        }

        private void processFields() {
            for (final PsiField field : psiClass.getPsiClass().getAllFields()) {
                addTask(field, new RunOnce() {
                    @Override
                    public void doRun() {
                        processField(field);
                    }
                });
            }
        }

        private void processMethods() {
            parseAllMethodsAsProperties();
            processOwnMethods();
        }

        private void processOwnMethods() {
            for (final PsiMethod method : psiClass.getPsiClass().getMethods()) {
                RunOnce task = new RunOnce() {
                    @Override
                    public void doRun() {
                        processOwnMethod(method);
                    }
                };
                addTask(method, task);

                PropertyParseResult propertyParseResult = PropertyNameUtils.parseMethodToProperty(method.getName());
                if (propertyParseResult != null) {
                    addTask(propertyParseResult.getPropertyName(), task);
                }
            }
        }

        private void parseAllMethodsAsProperties() {
            for (PsiMethod method : psiClass.getPsiClass().getAllMethods()) {
                createEmptyEntry(Name.identifier(method.getName()));

                PropertyParseResult propertyParseResult = PropertyNameUtils.parseMethodToProperty(method.getName());
                if (propertyParseResult != null) {
                    createEmptyEntry(Name.identifier(propertyParseResult.getPropertyName()));
                }
            }
        }

        private void processNestedClasses() {
            if (!staticMembers) {
                return;
            }
            for (final PsiClass nested : psiClass.getPsiClass().getInnerClasses()) {
                addTask(nested, new RunOnce() {
                    @Override
                    public void doRun() {
                        processNestedClass(nested);
                    }
                });
            }
        }

        private boolean includeMember(PsiMemberWrapper member) {
            if (psiClass.getPsiClass().isEnum() && staticMembers) {
                return member.isStatic();
            }

            if (member.isStatic() != staticMembers) {
                return false;
            }

            if (member.getPsiMember().getContainingClass() != psiClass.getPsiClass()) {
                return false;
            }

            if (member.isPrivate()) {
                return false;
            }

            if (isObjectMethodInInterface(member.getPsiMember())) {
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

            TypeSource type = new TypeSource("", fieldWrapper.getType(), field);
            namedMembers.addPropertyAccessor(new PropertyPsiDataElement(fieldWrapper, type, null));
        }

        private void processOwnMethod(PsiMethod ownMethod) {
            PsiMethodWrapper method = new PsiMethodWrapper(ownMethod);

            if (!includeMember(method)) {
                return;
            }

            PropertyParseResult propertyParseResult = PropertyNameUtils.parseMethodToProperty(method.getName());

            // TODO: remove getJavaClass
            if (propertyParseResult != null && propertyParseResult.isGetter()) {
                processGetter(ownMethod, method, propertyParseResult);
            }
            else if (propertyParseResult != null) {
                processSetter(method, propertyParseResult);
            }

            NamedMembers namedMembers = getOrCreateEmpty(Name.identifier(method.getName()));
            namedMembers.addMethod(method);
        }

        //TODO: manually!!!!
        private void processSetter(PsiMethodWrapper method, PropertyParseResult propertyParseResult) {
            //TODO
            String propertyName = propertyParseResult.getPropertyName();
            getOrCreateEmpty(Name.identifier(propertyName));
        }

        private void processGetter(PsiMethod ownMethod, PsiMethodWrapper method, PropertyParseResult propertyParseResult) {
            //TODO:
            String propertyName = propertyParseResult.getPropertyName();
            getOrCreateEmpty(Name.identifier(propertyName));
        }

        private void createEmptyEntry(@NotNull Name identifier) {
            getOrCreateEmpty(identifier);
        }

        private void processNestedClass(PsiClass nested) {
            if (isSamInterface(nested)) {
                NamedMembers namedMembers = getOrCreateEmpty(Name.identifier(nested.getName()));
                namedMembers.setSamInterface(nested);
            }
        }
    }

    public static boolean isObjectMethodInInterface(@NotNull PsiMember member) {
        if (!(member instanceof PsiMethod)) {
            return false;
        }
        PsiClass containingClass = member.getContainingClass();
        assert containingClass != null : "containing class is null for " + member;

        if (!containingClass.isInterface()) {
            return false;
        }

        return isObjectMethod((PsiMethod) member);
    }

    private static boolean isObjectMethod(PsiMethod method) {
        String formattedMethod = PsiFormatUtil.formatMethod(
                method, PsiSubstitutor.EMPTY, SHOW_NAME | SHOW_PARAMETERS, SHOW_TYPE | SHOW_FQ_CLASS_NAMES);
        return OBJECT_METHODS.contains(formattedMethod);
    }

    public static boolean isSamInterface(@NotNull PsiClass psiClass) {
        return getSamInterfaceMethod(psiClass) != null;
    }

    // Returns null if not SAM interface
    @Nullable
    public static PsiMethod getSamInterfaceMethod(@NotNull PsiClass psiClass) {
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null || qualifiedName.startsWith(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.asString() + ".")) {
            return null;
        }
        if (!psiClass.isInterface() || psiClass.isAnnotationType()) {
            return null;
        }

        return findOnlyAbstractMethod(psiClass);
    }

    @Nullable
    private static PsiMethod findOnlyAbstractMethod(@NotNull PsiClass psiClass) {
        PsiClassType classType = JavaPsiFacade.getElementFactory(psiClass.getProject()).createType(psiClass);

        OnlyAbstractMethodFinder finder = new OnlyAbstractMethodFinder();
        if (finder.find(classType)) {
            return finder.getFoundMethod();
        }
        return null;
    }

    private static boolean isVarargMethod(@NotNull PsiMethod method) {
        PsiParameter lastParameter = ArrayUtil.getLastElement(method.getParameterList().getParameters());
        return lastParameter != null && lastParameter.getType() instanceof PsiEllipsisType;
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

    private static class OnlyAbstractMethodFinder {
        private MethodSignatureBackedByPsiMethod found;

        private boolean find(@NotNull PsiClassType classType) {
            PsiClassType.ClassResolveResult classResolveResult = classType.resolveGenerics();
            PsiSubstitutor classSubstitutor = classResolveResult.getSubstitutor();
            PsiClass psiClass = classResolveResult.getElement();
            if (psiClass == null) {
                return false; // can't resolve class -> not a SAM interface
            }
            if (CommonClassNames.JAVA_LANG_OBJECT.equals(psiClass.getQualifiedName())) {
                return true;
            }
            for (PsiMethod method : psiClass.getMethods()) {
                if (isObjectMethod(method)) { // e.g., ignore toString() declared in interface
                    continue;
                }
                if (method.hasTypeParameters()) {
                    return false; // if interface has generic methods, it is not a SAM interface
                }

                if (found == null) {
                    found = (MethodSignatureBackedByPsiMethod) method.getSignature(classSubstitutor);
                    continue;
                }
                if (!found.getName().equals(method.getName())) {
                    return false; // optimizing heuristic
                }
                MethodSignatureBackedByPsiMethod current = (MethodSignatureBackedByPsiMethod) method.getSignature(classSubstitutor);
                if (!areSignaturesErasureEqual(current, found) || isVarargMethod(method) != isVarargMethod(found.getMethod())) {
                    return false; // different signatures
                }
            }

            for (PsiType t : classType.getSuperTypes()) {
                if (!find((PsiClassType) t)) {
                    return false;
                }
            }

            return true;
        }

        @Nullable
        PsiMethod getFoundMethod() {
            return found == null ? null : found.getMethod();
        }
    }
}
