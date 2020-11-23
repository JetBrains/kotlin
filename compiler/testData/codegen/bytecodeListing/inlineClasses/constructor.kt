// !LANGUAGE: +InlineClasses

inline class IC1 public constructor(val i: Int)
inline class IC11 internal constructor(val i: Int)
inline class IC2 private constructor(val i: Int)
inline class IC4 protected constructor(val i: Int)

private inline class PIC1 public constructor(val i: Int)
private inline class PIC11 internal constructor(val i: Int)
private inline class PIC2 private constructor(val i: Int)
private inline class PIC4 protected constructor(val i: Int)