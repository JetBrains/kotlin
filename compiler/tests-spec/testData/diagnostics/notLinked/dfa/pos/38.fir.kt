// !DIAGNOSTICS: -UNUSED_EXPRESSION
// SKIP_TXT

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: dfa
 * NUMBER: 38
 * DESCRIPTION: Raw data flow analysis test
 * HELPERS: classes, objects, typealiases, functions, enumClasses, interfaces, sealedClasses
 */

// TESTCASE NUMBER: 1
fun case_1(x: Comparable<*>?) {
    if (x is Byte?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>? & kotlin.Byte?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Comparable<*>? & kotlin.Byte?")!>x<!>?.equals(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Byte")!>x!!<!>.dec()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: ClassWithThreeTypeParameters<*, *, *>?) {
    if (x is InterfaceWithTwoTypeParameters<*, *>?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *>? & ClassWithThreeTypeParameters<*, *, *>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *>? & ClassWithThreeTypeParameters<*, *, *>?")!>x<!>?.x
        x?.y
        x?.z
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x!!<!>.ip2test()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x<!>.x
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: ClassWithThreeTypeParameters<*, *, *>) {
    if (x is InterfaceWithTwoTypeParameters<*, *>?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x<!>.x
        x.y
        x.z
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>.ip2test()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x<!>.x
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: ClassWithSixTypeParameters<*, *, *, *, *, *>?) {
    if (x is InterfaceWithTwoTypeParameters<*, *>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>? & InterfaceWithTwoTypeParameters<*, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>? & InterfaceWithTwoTypeParameters<*, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>")!>x<!>.x
        x.y
        x.z
        x.u
        <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<*, *> & ClassWithSixTypeParameters<*, kotlin.Nothing, *, *, kotlin.Nothing, *>")!>x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>.ip2test()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>? & InterfaceWithTwoTypeParameters<*, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *>? & InterfaceWithTwoTypeParameters<*, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>")!>x<!>.x
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: ClassWithThreeTypeParameters<*, *, *>?) {
    if (x is InterfaceWithTwoTypeParameters<*, *>?) {
        if (x === null) return
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x<!>.x
        x.y
        x.z
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x<!>.ip2test()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *> & ClassWithThreeTypeParameters<*, *, *>")!>x<!>.x
    }
}

// TESTCASE NUMBER: 6
fun case_5(x: Any?) {
    if (x is ClassWithThreeTypeParameters<*, *, *>?) {
        if (x is InterfaceWithTwoTypeParameters<*, *>?) {
            if (x === null) return
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *>")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *>")!>x<!>.x
            x.y
            x.z
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *>")!>x<!>.ip2test()
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *>")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any? & ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *>")!>x<!>.x
        }
    }
}
