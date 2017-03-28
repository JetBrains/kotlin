interface Test1<<!REDECLARATION!>T<!>, <!REDECLARATION!>T<!>>
interface Test2<<!REDECLARATION!>X<!>, Y, <!REDECLARATION!>X<!>>

class Outer<T> {
    interface TestNested<T>
}
