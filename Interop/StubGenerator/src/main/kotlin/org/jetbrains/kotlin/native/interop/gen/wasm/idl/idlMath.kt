package org.jetbrains.kotlin.native.interop.gen.wasm.idl

// There are no WebIDL descriptions of Math,
// so in any case this one will be a part of the project.
// Although, may be in the form of our own WebIDL source.

val idlMath = listOf(
    Interface("Math",
        Attribute("E",      idlDouble,  readOnly = true, isStatic = true),
        Attribute("LN2",    idlDouble,  readOnly = true, isStatic = true),
        Attribute("LN10",   idlDouble,  readOnly = true, isStatic = true),
        Attribute("LOG2E",  idlDouble,  readOnly = true, isStatic = true),
        Attribute("LOG10E", idlDouble,  readOnly = true, isStatic = true),
        Attribute("PI",     idlDouble,  readOnly = true, isStatic = true),
        Attribute("SQRT1_2", idlDouble, readOnly = true, isStatic = true),
        Attribute("SQRT2",  idlDouble,  readOnly = true, isStatic = true),

        Operation("abs",    idlDouble,  true,  Arg("x", idlDouble)),
        Operation("acos",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("acosh",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("asin",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("asinh",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("atan",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("atanh",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("atan2",  idlDouble,  true,  Arg("y", idlDouble), Arg("x", idlDouble)),
        Operation("cbrt",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("ceil",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("clz32",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("cos",    idlDouble,  true,  Arg("x", idlDouble)),
        Operation("cosh",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("exp",    idlDouble,  true,  Arg("x", idlDouble)),
        Operation("expm1",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("floor",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("fround", idlDouble,  true,  Arg("x", idlDouble)),
        //Operation("hypot([x[, y[, …]]]),
        //Operation("imul(x,    y),
        Operation("log",    idlDouble,  true,  Arg("x", idlDouble)),
        Operation("log1p",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("log10",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("log2",   idlDouble,  true,  Arg("x", idlDouble)),
        //Operation("max([x[,   y[, …]]]),  // TODO: Support varargs.
        //Operation("min([x[,   y[, …]]]),
        Operation("pow",    idlDouble,  true,  Arg("x", idlDouble), Arg("y", idlDouble)),
        Operation("random", idlDouble,  true),
        Operation("round",  idlDouble,  true,  Arg("x", idlDouble)),
        Operation("sign",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("sin",    idlDouble,  true,  Arg("x", idlDouble)),
        Operation("sinh",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("sqrt",   idlDouble,  true,  Arg("x", idlDouble)),
        Operation("tan",    idlDouble,  true,  Arg("x", idlDouble)),
        Operation("tanh",   idlDouble,  true,  Arg("x", idlDouble)),
        //Operation("toSource(),
        Operation("trunc",  idlDouble,  true,  Arg("x", idlDouble))
    )
)
