fun f1() {
    f2(<!ELEMENT!> = expr)

    f3(<!ELEMENT!> = (0L - 10 + throw E() - -.09))

    f4(
        <!ELEMENT!> = return return,
        <!ELEMENT!> = try {} finally {},
        <!ELEMENT!> = 0x10
    )
}
