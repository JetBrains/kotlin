package org.jetbrains.jet.codegen;

import gnu.trove.TObjectIntHashMap;
import org.jetbrains.jet.lang.types.DeclarationDescriptor;
import org.objectweb.asm.Type;

/**
 * @author max
 */
public class FrameMap {
    private final TObjectIntHashMap<DeclarationDescriptor> myVarIndex = new TObjectIntHashMap<DeclarationDescriptor>();
    private final TObjectIntHashMap<DeclarationDescriptor> myVarSizes = new TObjectIntHashMap<DeclarationDescriptor>();
    private int myMaxIndex = 0;

    public void enter(DeclarationDescriptor descriptor, int size) {
        myVarIndex.put(descriptor, myMaxIndex);
        myMaxIndex += size;
        myVarSizes.put(descriptor, size);
    }

    public int leave(DeclarationDescriptor descriptor) {
        int size = myVarSizes.get(descriptor);
        myMaxIndex -= size;
        myVarSizes.remove(descriptor);
        return myVarIndex.remove(descriptor);
    }

    public int enterTemp() {
        return myMaxIndex++;
    }

    public void leaveTemp() {
        myMaxIndex--;
    }

    public int getIndex(DeclarationDescriptor descriptor) {
        return myVarIndex.contains(descriptor) ? myVarIndex.get(descriptor) : -1;
    }

}
