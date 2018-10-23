class <!ELEMENT!> {}

internal class A: <!ELEMENT!> {}

interface <!ELEMENT!> {}

interface A: @<!ELEMENT!> B {}

interface A: B, C, <!ELEMENT!> {}
