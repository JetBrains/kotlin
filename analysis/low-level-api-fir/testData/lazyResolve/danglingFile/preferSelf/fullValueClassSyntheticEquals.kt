// LANGUAGE: +FullValueClasses
// MEMBER_NAME_FILTER: equals
package one

class TopLevel {
    value class Poi<caret>nt(val x: Int, val y: Int) : InterfaceWithParameter<Point>
}

interface InterfaceWithParameter<T>
