// !LANGUAGE: +NewInference
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
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Byte? & kotlin.Comparable<*>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Byte? & kotlin.Comparable<*>?")!>x<!>?.equals(10)
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Byte")!><!DEBUG_INFO_SMARTCAST!>x<!>!!<!>.dec()
    }
}

// TESTCASE NUMBER: 2
fun case_2(x: ClassWithThreeTypeParameters<*, *, *>?) {
    if (x is InterfaceWithTwoTypeParameters<*, *>?) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *>?")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *>?")!>x<!>?.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x?.y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x?.z<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("{ClassWithThreeTypeParameters<out Any?, out Any?, out Any?> & InterfaceWithTwoTypeParameters<out Any?, out Any?>}")!>x!!<!>.ip2test()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *>")!>x<!>.x<!>
    }
}

// TESTCASE NUMBER: 3
fun case_3(x: ClassWithThreeTypeParameters<*, *, *>) {
    if (x is InterfaceWithTwoTypeParameters<*, *><!USELESS_NULLABLE_CHECK!>?<!>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *>")!>x<!>.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x.y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x.z<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("{ClassWithThreeTypeParameters<out Any?, out Any?, out Any?> & InterfaceWithTwoTypeParameters<out Any?, out Any?>}")!>x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>.ip2test()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *>")!>x<!>.x<!>
    }
}

// TESTCASE NUMBER: 4
fun case_4(x: ClassWithSixTypeParameters<*, *, *, *, *, *>?) {
    if (x is InterfaceWithTwoTypeParameters<*, *>) {
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>? & InterfaceWithTwoTypeParameters<*, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>? & InterfaceWithTwoTypeParameters<*, *>")!>x<!>.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x.y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x.z<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x.u<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("{ClassWithSixTypeParameters<out Any?, Nothing, Any?, out Any?, Nothing, Any?> & InterfaceWithTwoTypeParameters<out Any?, out Any?>}")!>x<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!><!>.ip2test()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>? & InterfaceWithTwoTypeParameters<*, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithSixTypeParameters<*, *, *, *, *, *> & ClassWithSixTypeParameters<*, *, *, *, *, *>? & InterfaceWithTwoTypeParameters<*, *>")!>x<!>.x<!>
    }
}

// TESTCASE NUMBER: 5
fun case_5(x: ClassWithThreeTypeParameters<*, *, *>?) {
    if (x is InterfaceWithTwoTypeParameters<*, *>?) {
        if (x === null) return
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *>")!>x<!>.x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x.y<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!>x.z<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *>")!>x<!>.ip2test()
        <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *>")!>x<!>
        <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & ClassWithThreeTypeParameters<*, *, *>? & InterfaceWithTwoTypeParameters<*, *>")!>x<!>.x<!>
    }
}

// TESTCASE NUMBER: 6
fun case_5(x: Any?) {
    if (x is ClassWithThreeTypeParameters<*, *, *>?) {
        if (x is InterfaceWithTwoTypeParameters<*, *>?) {
            if (x === null) return
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *> & kotlin.Any & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_SMARTCAST!>x<!>.y<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_SMARTCAST!>x<!>.z<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("InterfaceWithTwoTypeParameters<*, *> & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.ip2test()
            <!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & InterfaceWithTwoTypeParameters<*, *> & kotlin.Any & kotlin.Any?")!>x<!>
            <!DEBUG_INFO_EXPRESSION_TYPE("kotlin.Any?")!><!DEBUG_INFO_EXPRESSION_TYPE("ClassWithThreeTypeParameters<*, *, *> & kotlin.Any?"), DEBUG_INFO_SMARTCAST!>x<!>.x<!>
        }
    }
}
