package org.jetbrains.jet.lang.psi.stubs.impl;

import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetClassElementType;

/**
 * @author Nikolay Krasko
 */
public class PsiJetClassStubImpl extends StubBase<JetClass> implements PsiJetClassStub<JetClass> {

    private final StringRef qualifiedName;
    private final StringRef name;

    public PsiJetClassStubImpl(
            JetClassElementType type,
            final StubElement parent,
            final String qualifiedName,
            final String name) {

        this(type, parent, StringRef.fromString(qualifiedName), StringRef.fromString(name));
    }

    public PsiJetClassStubImpl(
            JetClassElementType type,
            final StubElement parent,
            final StringRef qualifiedName,
            final StringRef name) {

        super(parent, type);
        this.qualifiedName = qualifiedName;
        this.name = name;
    }

    @Override
    public String getQualifiedName() {
        return StringRef.toString(qualifiedName);
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public boolean hasDeprecatedAnnotation() {
        return false;
    }

    @Override
    public String getName() {
        return StringRef.toString(name);
    }
}
