package org.jetbrains.jet.codegen;

import org.objectweb.asm.commons.InstructionAdapter;

/**
 * @author yole
 */
public interface CodeChunk {
    void generate(InstructionAdapter v);
}
