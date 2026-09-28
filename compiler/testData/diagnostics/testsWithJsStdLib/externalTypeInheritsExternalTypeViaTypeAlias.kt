// RUN_PIPELINE_TILL: CODEGEN

external interface Base

typealias TypeAlias = Base

external interface Derived: TypeAlias
