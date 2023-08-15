// NON_PUBLIC_MARKERS: one.two/Three.Four five.six/Seven.Eight nine.ten/Eleven.Twelve
// MODULE: with_non_public_markers_library

annotation class Foo
annotation class Bar

object Another {
    annotation class Foo
    annotation class Bar
}

class NonMarkedClass {
    class NonMarkedClass {
        class NonMarkedClass
        @Foo class ClassMarkedWithFoo
        @Bar class ClassMarkedWithAnotherFoo
        @Another.Foo class ClassMarkedWithBar
        @Another.Bar class ClassMarkedWithAnotherBar
    }
    @Foo class ClassMarkedWithFoo
    @Bar class ClassMarkedWithAnotherFoo
    @Another.Foo class ClassMarkedWithBar
    @Another.Bar class ClassMarkedWithAnotherBar
}
@Foo class ClassMarkedWithFoo {
    class NonMarkedClass {
        class NonMarkedClass
        @Foo class ClassMarkedWithFoo
        @Bar class ClassMarkedWithAnotherFoo
        @Another.Foo class ClassMarkedWithBar
        @Another.Bar class ClassMarkedWithAnotherBar
    }
    @Foo class ClassMarkedWithFoo
    @Bar class ClassMarkedWithAnotherFoo
    @Another.Foo class ClassMarkedWithBar
    @Another.Bar class ClassMarkedWithAnotherBar
}
@Bar class ClassMarkedWithAnotherFoo {
    class NonMarkedClass {
        class NonMarkedClass
        @Foo class ClassMarkedWithFoo
        @Bar class ClassMarkedWithAnotherFoo
        @Another.Foo class ClassMarkedWithBar
        @Another.Bar class ClassMarkedWithAnotherBar
    }
    @Foo class ClassMarkedWithFoo
    @Bar class ClassMarkedWithAnotherFoo
    @Another.Foo class ClassMarkedWithBar
    @Another.Bar class ClassMarkedWithAnotherBar
}
@Another.Foo class ClassMarkedWithBar {
    class NonMarkedClass {
        class NonMarkedClass
        @Foo class ClassMarkedWithFoo
        @Bar class ClassMarkedWithAnotherFoo
        @Another.Foo class ClassMarkedWithBar
        @Another.Bar class ClassMarkedWithAnotherBar
    }
    @Foo class ClassMarkedWithFoo
    @Bar class ClassMarkedWithAnotherFoo
    @Another.Foo class ClassMarkedWithBar
    @Another.Bar class ClassMarkedWithAnotherBar
}
@Another.Bar class ClassMarkedWithAnotherBar {
    class NonMarkedClass {
        class NonMarkedClass
        @Foo class ClassMarkedWithFoo
        @Bar class ClassMarkedWithAnotherFoo
        @Another.Foo class ClassMarkedWithBar
        @Another.Bar class ClassMarkedWithAnotherBar
    }
    @Foo class ClassMarkedWithFoo
    @Bar class ClassMarkedWithAnotherFoo
    @Another.Foo class ClassMarkedWithBar
    @Another.Bar class ClassMarkedWithAnotherBar
}

class ClassWithConstructorMarkedWithFoo @Foo constructor()
class ClassWithConstructorMarkedWithAnotherFoo @Another.Foo constructor()
class ClassWithConstructorMarkedWithBar @Bar constructor()
class ClassWithConstructorMarkedWithAnotherBar @Another.Bar constructor()

fun nonMarkedFunction(): String = ""
@Foo fun functionMarkedWithFoo(): String = ""
@Bar fun functionMarkedWithAnotherFoo(): String = ""
@Another.Foo fun functionMarkedWithBar(): String = ""
@Another.Bar fun functionMarkedWithAnotherBar(): String = ""

var nonMarkedProperty: String get() = ""
    set(_) = Unit

@Foo var propertyWholeMarkedWithFoo: String get() = ""
    set(_) = Unit
@Another.Foo var propertyWholeMarkedWithAnotherFoo: String get() = ""
    set(_) = Unit
@Bar var propertyWholeMarkedWithBar: String get() = ""
    set(_) = Unit
@Another.Bar var propertyWholeMarkedWithAnotherBar: String get() = ""
    set(_) = Unit

var propertyGetterMarkedWithFoo: String @Foo get() = ""
    set(_) = Unit
var propertyGetterMarkedWithAnotherFoo: String @Another.Foo get() = ""
    set(_) = Unit
var propertyGetterMarkedWithBar: String @Bar get() = ""
    set(_) = Unit
var propertyGetterMarkedWithAnotherBar: String @Another.Bar get() = ""
    set(_) = Unit

var propertySetterMarkedWithFoo: String get() = ""
    @Foo set(_) = Unit
var propertySetterMarkedWithAnotherFoo: String get() = ""
    @Another.Foo set(_) = Unit
var propertySetterMarkedWithBar: String get() = ""
    @Bar set(_) = Unit
var propertySetterMarkedWithAnotherBar: String get() = ""
    @Another.Bar set(_) = Unit
