package org.jetbrains.jet.codegen;

import gnu.trove.TObjectIntHashMap;
import org.jetbrains.jet.lang.types.DeclarationDescriptor;

/**
 * @author max
 */
public class FrameMap {
    private final TObjectIntHashMap<DeclarationDescriptor> myVarIndex = new TObjectIntHashMap<DeclarationDescriptor>();
    private int myMaxIndex = 0;

    public void enter(DeclarationDescriptor descriptor) {
        myVarIndex.put(descriptor, myMaxIndex++);
    }

    public int leave(DeclarationDescriptor descriptor) {
        myMaxIndex--;
        return myVarIndex.remove(descriptor);
    }

    public int getIndex(DeclarationDescriptor descriptor) {
        return myVarIndex.get(descriptor);
    }

}
