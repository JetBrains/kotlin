// ENABLE_EXPECT_ACTUAL_CLASSES_WARNING
// MODULE: m1-common
// FILE: common.kt

<!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING, EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING{JVM}!>expect<!> class Clazz {
    class <!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING, EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING{JVM}!>Nested<!>

    fun memberFun()
    val memberProp: Clazz
}

<!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING, EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING{JVM}!>expect<!> interface Interface

<!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING, EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING{JVM}!>expect<!> object Object

<!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING, EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING{JVM}!>expect<!> annotation class Annotation

<!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING, EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING{JVM}!>expect<!> enum class Enum

<!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING, EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING{JVM}!>expect<!> class ActualTypealias

expect fun function()

expect val property: Clazz

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
<!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING!>actual<!> class Clazz {
    <!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING!>actual<!> class Nested

    actual fun memberFun() {}
    actual val memberProp: Clazz = null!!
}

<!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING!>actual<!> interface Interface

<!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING!>actual<!> object Object

<!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING!>actual<!> annotation class Annotation

<!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING!>actual<!> enum class Enum

<!EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING!>actual<!> typealias ActualTypealias = ActualTypealiasImpl

class ActualTypealiasImpl

actual fun function() {}

actual val property: Clazz = null!!
