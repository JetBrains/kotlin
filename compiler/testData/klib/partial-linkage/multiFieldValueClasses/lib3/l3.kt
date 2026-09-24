value class OneToTwoParams(val first: Int)

value class TwoToOneParams(val first: Int, val second: Int)

value class MfvcToAbstract(val first: Int, val second: Int)

abstract value class AbstractToMfvc(first: Int, second: Int)

class ClassToMfvc(val first: Int, val second: Int)

value class MfvcToClass(val first: Int, val second: Int)

value class MfvcVersionOverload(val first: Int)
