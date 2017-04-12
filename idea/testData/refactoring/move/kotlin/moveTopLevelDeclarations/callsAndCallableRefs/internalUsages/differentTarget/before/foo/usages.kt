package foo

import foo.O.objectMember2
import foo.O.objectExtensionMember2

class X

fun <caret>test() {
    A().classMember()
    A().classExtension()
    O.objectMember1()
    objectMember2()
    O.objectExtension()
    A.companionMember()
    A.companionExtension()
    J().javaClassMember()
    J.javaClassStaticMember()
    topLevel()
    with(O) { 1.objectExtensionMember1() }
    1.objectExtensionMember2()
    with(A) { 1.companionExtensionMember() }

    A()::classMember
    A::classMember
    A()::classExtension
    A::classExtension
    O::objectMember1
    ::objectMember2
    O::objectExtension
    A.Companion::companionMember
    (A)::companionMember
    A.Companion::companionExtension
    (A)::companionExtension
    J()::javaClassMember
    J::javaClassMember
    J::javaClassStaticMember
    ::topLevel

    with(A()) {
        classMember()
        this.classMember()
        classExtension()
        this.classExtension()

        this::classMember
        this::classExtension
    }

    with(J()) {
        javaClassMember()
        this.javaClassMember()

        this::javaClassMember
    }
}