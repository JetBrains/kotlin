package org.jetbrains.jet.plugin;

import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.java.JavaClassDescriptor;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.DeferredType;
import org.jetbrains.jet.lang.types.JetStandardLibrary;
import org.jetbrains.jet.lang.types.JetType;

import java.util.LinkedList;

/**
 * @author svtk
 */
public class JetPluginUtil {
    @NotNull
    public static String computeTypeFullName(JetType type) {
        LinkedList<String> fullName = computeTypeFullNameList(type);
        String last = fullName.getLast();
        StringBuilder sb = new StringBuilder();
        for (String s : fullName) {
            sb.append(s);
            if (s != last) {
                sb.append('.');
            }
        }
        return sb.toString();
    }

    @NotNull
    private static LinkedList<String> computeTypeFullNameList(JetType type) {
        if (type instanceof DeferredType) {
            type = ((DeferredType)type).getActualType();
        }
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();

        LinkedList<String> fullName = Lists.newLinkedList();
        while (declarationDescriptor != null) {
            fullName.addFirst(declarationDescriptor.getName());
            declarationDescriptor = declarationDescriptor.getContainingDeclaration();
        }
        assert fullName.size() > 0;
        if (JavaDescriptorResolver.JAVA_ROOT.equals(fullName.getFirst())) {
            fullName.removeFirst();
        }
        return fullName;
    }

    public static boolean checkTypeIsStandard(JetType type, Project project) {
        LinkedList<String> fullName = computeTypeFullNameList(type);
        if (fullName.size() == 3 && fullName.getFirst().equals("java") && fullName.get(1).equals("lang")) {
            return true;
        }

        JetStandardLibrary standardLibrary = JetStandardLibrary.getJetStandardLibrary(project);
        JetScope libraryScope = standardLibrary.getLibraryScope();

        DeclarationDescriptor declaration = type.getMemberScope().getContainingDeclaration();
        if (declaration instanceof JavaClassDescriptor) {
            return false;
        }
        while (!(declaration instanceof NamespaceDescriptor)) {
            declaration = declaration.getContainingDeclaration();
            assert declaration != null;
        }
        return libraryScope == ((NamespaceDescriptor) declaration).getMemberScope();
    }
}
