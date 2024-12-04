package my

interface BaseInterface1

interface BaseInterface2

interface ComplexInterface : BaseInterface1, BaseInterface2

open class OpenBaseClass

class FinalClassWithBaseInterface : BaseInterface1

class FinalClassWithSeveralBaseInterfaces : BaseInterface1, BaseInterface2

class FinalClassWithComplexInterface : ComplexInterface

class FinalClassWithComplexInterfaceAndBaseInterface : ComplexInterface, BaseInterface1

abstract class AbstractClassWithBaseInterface : BaseInterface2

abstract class AbstractClassWithComplexInterface : ComplexInterface

abstract class AbstractClassTransitiveBaseInterface : AbstractClassWithBaseInterface(), BaseInterface1

open class OpenComplexClass : ComplexInterface, AbstractClassTransitiveBaseInterface()

class OnlyTransitiveInterface : OpenComplexClass()
