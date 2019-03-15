// FILE: lib.kt

package lib

class TestObserver<T> {
    fun assertValue(valuePredicate: (T) -> Boolean): Unit = TODO()
}

class Single<T> {
    fun test(): TestObserver<T> = TODO()
}

class Employee

class Either<T>

typealias DomainEither<T> = Either<T>
typealias DomainSingle<T> = Single<DomainEither<T>>

fun provideDomainSingle(): DomainSingle<Employee> = TODO()

class CreateEmployeeUseCaseAccessor {
    fun testNormalName() {
        val testObs = provideDomainSingle().test()
        testObs.assertValue { true }
    }
}


// FILE: main.kt

import lib.*

class CreateEmployeeUseCaseTest {
    fun testNormalName() {
        val testObs = provideDomainSingle().test()
        testObs.assertValue { true }
    }
}

fun box(): String {
    return "OK"
}