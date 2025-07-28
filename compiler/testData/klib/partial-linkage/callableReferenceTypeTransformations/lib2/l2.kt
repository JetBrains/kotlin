fun getFunctionalInterfaceToInterface(answer: Int): FunctionalInterfaceToInterface {
    val worker = FunctionalInterfaceToInterface { answer }
    return worker
}
fun getFunctionalInterfaceToInterfaceAsSamConverted(answer: Int): FunctionalInterfaceToInterface {
    val worker = { answer }
    return FunctionalInterfaceToInterface(worker)
}
fun getFunctionalInterfaceToInterfaceAsObject(answer: Int): FunctionalInterfaceToInterface {
    val worker = object : FunctionalInterfaceToInterface {
        override fun answer() = answer
    }
    return worker
}
fun getFunctionalInterfaceToInterfaceAnswer(answer: Int): Int {
    return getFunctionalInterfaceToInterface(answer).answer()
}

fun getFunInterfaceWithChangedFun(answer: Int): FunInterfaceWithChangedFun {
    val worker = FunInterfaceWithChangedFun { answer }
    return worker
}
fun getFunInterfaceWithChangedFunAsSamConverted(answer: Int): FunInterfaceWithChangedFun {
    val worker = { answer }
    return FunInterfaceWithChangedFun(worker)
}
fun getFunInterfaceWithChangedFunAsObject(answer: Int): FunInterfaceWithChangedFun {
    val worker = object : FunInterfaceWithChangedFun {
        override fun answer() = answer
    }
    return worker
}
fun getFunInterfaceWithChangedFunAnswer(answer: Int): Int {
    return getFunInterfaceWithChangedFun(answer).answer()
}

fun getFunInterfaceWithDifferentAbstractFun(answer: Int): FunInterfaceWithDifferentAbstractFun {
    val worker = FunInterfaceWithDifferentAbstractFun { answer }
    return worker
}
fun getFunInterfaceWithDifferentAbstractFunAsSamConverted(answer: Int): FunInterfaceWithDifferentAbstractFun {
    val worker = { answer }
    return FunInterfaceWithDifferentAbstractFun(worker)
}
fun getFunInterfaceWithDifferentAbstractFunAsObject(answer: Int): FunInterfaceWithDifferentAbstractFun {
    val worker = object : FunInterfaceWithDifferentAbstractFun {
        override fun answer() = answer
    }
    return worker
}
fun getFunInterfaceWithDifferentAbstractFunAnswer(answer: Int): Int {
    return getFunInterfaceWithDifferentAbstractFun(answer).answer()
}

fun getFunInterfaceWithDifferentChangedAbstractFun(answer: Int): FunInterfaceWithDifferentChangedAbstractFun {
    val worker = FunInterfaceWithDifferentChangedAbstractFun { answer }
    return worker
}
fun getFunInterfaceWithDifferentChangedAbstractFunAsSamConverted(answer: Int): FunInterfaceWithDifferentChangedAbstractFun {
    val worker = { answer }
    return FunInterfaceWithDifferentChangedAbstractFun(worker)
}
fun getFunInterfaceWithDifferentChangedAbstractFunAsObject(answer: Int): FunInterfaceWithDifferentChangedAbstractFun {
    val worker = object : FunInterfaceWithDifferentChangedAbstractFun {
        override fun answer() = answer
    }
    return worker
}
fun getFunInterfaceWithDifferentChangedAbstractFunAnswer(answer: Int): Int {
    return getFunInterfaceWithDifferentChangedAbstractFun(answer).answer()
}

fun interface FunctionalInterfaceWith0AbstractFunctions : XAnswerDefault
fun interface FunctionalInterfaceWith1AbstractFunction : XAnswer, XFunction1Default, XFunction2Default, XProperty1Default, XProperty2Default
fun interface FunctionalInterfaceWith2AbstractFunctions : XAnswer, XFunction1, XFunction2Default, XProperty1Default, XProperty2Default
fun interface FunctionalInterfaceWith3AbstractFunctions : XAnswer, XFunction1, XFunction2, XProperty1Default, XProperty2Default
fun interface FunctionalInterfaceWithAbstractProperty : XAnswer, XFunction1Default, XFunction2Default, XProperty1, XProperty2Default

fun getFunctionalInterfaceWith0AbstractFunctions(answer: Int): FunctionalInterfaceWith0AbstractFunctions {
    val worker = FunctionalInterfaceWith0AbstractFunctions { answer }
    return worker
}
fun getFunctionalInterfaceWith0AbstractFunctionsAsObject(answer: Int): FunctionalInterfaceWith0AbstractFunctions {
    val worker = object : FunctionalInterfaceWith0AbstractFunctions {
        override fun answer() = answer
    }
    return worker
}
fun getFunctionalInterfaceWith0AbstractFunctionsAsSamConverted(answer: Int): FunctionalInterfaceWith0AbstractFunctions {
    val worker = { answer }
    return FunctionalInterfaceWith0AbstractFunctions(worker)
}
fun getFunctionalInterfaceWith0AbstractFunctionsAnswer(answer: Int): Int {
    return getFunctionalInterfaceWith0AbstractFunctions(answer).answer()
}

fun getFunctionalInterfaceWith1AbstractFunction(answer: Int): FunctionalInterfaceWith1AbstractFunction {
    val worker = FunctionalInterfaceWith1AbstractFunction { answer }
    return worker
}
fun getFunctionalInterfaceWith1AbstractFunctionAsSamConverted(answer: Int): FunctionalInterfaceWith1AbstractFunction {
    val worker = { answer }
    return FunctionalInterfaceWith1AbstractFunction(worker)
}
fun getFunctionalInterfaceWith1AbstractFunctionAsObject(answer: Int): FunctionalInterfaceWith1AbstractFunction {
    val worker = object : FunctionalInterfaceWith1AbstractFunction {
        override fun answer() = answer
    }
    return worker
}
fun getFunctionalInterfaceWith1AbstractFunctionAnswer(answer: Int): Int {
    return getFunctionalInterfaceWith1AbstractFunction(answer).answer()
}

fun getFunctionalInterfaceWith2AbstractFunctions(answer: Int): FunctionalInterfaceWith2AbstractFunctions {
    val worker = FunctionalInterfaceWith2AbstractFunctions { answer }
    return worker
}
fun getFunctionalInterfaceWith2AbstractFunctionsAsSamConverted(answer: Int): FunctionalInterfaceWith2AbstractFunctions {
    val worker = { answer }
    return FunctionalInterfaceWith2AbstractFunctions(worker)
}
fun getFunctionalInterfaceWith2AbstractFunctionsAsObject(answer: Int): FunctionalInterfaceWith2AbstractFunctions {
    val worker = object : FunctionalInterfaceWith2AbstractFunctions {
        override fun answer() = answer
    }
    return worker
}
fun getFunctionalInterfaceWith2AbstractFunctionsAnswer(answer: Int): Int {
    return getFunctionalInterfaceWith2AbstractFunctions(answer).answer()
}

fun getFunctionalInterfaceWith3AbstractFunctions(answer: Int): FunctionalInterfaceWith3AbstractFunctions {
    val worker = FunctionalInterfaceWith3AbstractFunctions { answer }
    return worker
}
fun getFunctionalInterfaceWith3AbstractFunctionsAsSamConverted(answer: Int): FunctionalInterfaceWith3AbstractFunctions {
    val worker = { answer }
    return FunctionalInterfaceWith3AbstractFunctions(worker)
}
fun getFunctionalInterfaceWith3AbstractFunctionsAsObject(answer: Int): FunctionalInterfaceWith3AbstractFunctions {
    val worker = object : FunctionalInterfaceWith3AbstractFunctions {
        override fun answer() = answer
    }
    return worker
}
fun getFunctionalInterfaceWith3AbstractFunctionsAnswer(answer: Int): Int {
    return getFunctionalInterfaceWith3AbstractFunctions(answer).answer()
}

fun getFunctionalInterfaceWithAbstractProperty(answer: Int): FunctionalInterfaceWithAbstractProperty {
    val worker = FunctionalInterfaceWithAbstractProperty { answer }
    return worker
}
fun getFunctionalInterfaceWithAbstractPropertyAsSamConverted(answer: Int): FunctionalInterfaceWithAbstractProperty {
    val worker = { answer }
    return FunctionalInterfaceWithAbstractProperty(worker)
}
fun getFunctionalInterfaceWithAbstractPropertyAsObject(answer: Int): FunctionalInterfaceWithAbstractProperty {
    val worker = object : FunctionalInterfaceWithAbstractProperty {
        override fun answer() = answer
    }
    return worker
}
fun getFunctionalInterfaceWithAbstractPropertyAnswer(answer: Int): Int {
    return getFunctionalInterfaceWithAbstractProperty(answer).answer()
}
