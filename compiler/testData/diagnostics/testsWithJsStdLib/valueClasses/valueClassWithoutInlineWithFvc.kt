// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +FullValueClasses
// FIR_DUMP

value class IC(val x: Int)

value class IC2(val x: Int, val y: Int)

inline value class CorrectIC(val x: Int)
