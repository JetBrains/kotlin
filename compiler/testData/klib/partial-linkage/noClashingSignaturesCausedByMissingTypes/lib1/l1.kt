class RemovedClass
enum class RemovedEnum { A, B }

abstract class Base {
	fun funWithParameter(p: RemovedClass) {}
	fun <T : RemovedClass> funWithTypeParameter() {}

	fun funWithAnyParameter(p: Any) {}
	fun <T : Any> funWithAnyTypeParameter() {}
}

class Derived : Base()
