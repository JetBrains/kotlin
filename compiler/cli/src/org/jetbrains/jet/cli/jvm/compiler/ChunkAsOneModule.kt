package org.jetbrains.jet.cli.jvm.compiler

import jet.modules.Module

class ChunkAsOneModule(private val chunk: ModuleChunk) : Module {
    override fun getModuleName(): String = "chunk" + chunk.getModules().map { it.getModuleName() }.toString()

    override fun getOutputDirectory(): String {
        throw UnsupportedOperationException("Each module in a chunk has its own output directory")
    }
    override fun getSourceFiles(): List<String> = chunk.getModules().flatMap { it.getSourceFiles() }

    override fun getClasspathRoots(): List<String> = chunk.getModules().flatMap { it.getClasspathRoots() }

    override fun getAnnotationsRoots(): List<String> = chunk.getModules().flatMap { it.getAnnotationsRoots() }

}