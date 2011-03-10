package org.jetbrains.jet.lang.types;

/**
 * @author abreslav
 */
public interface DeclarationDescriptor extends Annotated, Named {
    /**
     * @return The descriptor that corresponds to the original declaration of this element.
     *         A descriptor can be obtained from its original by substituting type arguments (of the declaring class
     *         or of the element itself).
     *         returns <code>this</code> object if the current descriptor is original itself
     */
    DeclarationDescriptor getOriginal();

    <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data);
    void acceptVoid(DeclarationDescriptorVisitor<Void, Void> visitor);
}
