/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import java.io.PrintWriter
import java.io.StringWriter

open class TestClassBuilderFactory(
        private val classBuilderMode: ClassBuilderMode,
        private val jvmBackendClassResolver: JvmBackendClassResolver
) : ClassBuilderFactory {

    constructor(generateSourceRetentionAnnotations: Boolean, jvmBackendClassResolver: JvmBackendClassResolver) :
            this(ClassBuilderMode.full(generateSourceRetentionAnnotations), jvmBackendClassResolver)

    override fun getClassBuilderMode(): ClassBuilderMode = classBuilderMode

    override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder =
            TraceBuilder(BinaryClassWriter(jvmBackendClassResolver))

    override fun asText(builder: ClassBuilder): String {
        val visitor = builder.visitor as TraceClassVisitor
        val writer = StringWriter()
        visitor.p.print(PrintWriter(writer))
        return writer.toString()
    }

    override fun asBytes(builder: ClassBuilder): ByteArray =
            (builder as TraceBuilder).binary.toByteArray()

    override fun close() {}

    private class TraceBuilder(val binary: BinaryClassWriter) :
            AbstractClassBuilder.Concrete(TraceClassVisitor(binary, PrintWriter(StringWriter())))
}