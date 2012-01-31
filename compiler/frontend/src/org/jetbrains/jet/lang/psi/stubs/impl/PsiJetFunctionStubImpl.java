package org.jetbrains.jet.lang.psi.stubs.impl;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFunctionStub;

/**
 * @author Nikolay Krasko
 */
public class PsiJetFunctionStubImpl extends StubBase<JetFunction> implements PsiJetFunctionStub<JetFunction> {

    private final StringRef nameRef;
    private final boolean isTopLevel;
    private final boolean isExtension;
    
    public PsiJetFunctionStubImpl(@NotNull IStubElementType elementType, @NotNull StubElement parent,
                                  @Nullable String name, boolean isTopLevel, boolean isExtension) {
        this(elementType, parent, StringRef.fromString(name), isTopLevel, isExtension);
    }

    public PsiJetFunctionStubImpl(@NotNull IStubElementType elementType, @NotNull StubElement parent,
                                  @Nullable StringRef nameRef, boolean isTopLevel, boolean  isExtension) {
        super(parent, elementType);

        this.nameRef = nameRef;
        this.isTopLevel = isTopLevel;
        this.isExtension = isExtension;
    }

    @Override
    public String getName() {
        return StringRef.toString(nameRef);
    }

    @Override
    public boolean isTopLevel() {
        return isTopLevel;
    }

    @Override
    public boolean isExtension() {
        return isExtension;
    }

    @NotNull
    @Override
    public String[] getAnnotations() {
        // TODO (stubs)
        return new String[0];
    }
}
