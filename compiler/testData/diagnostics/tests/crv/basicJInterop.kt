// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

// FILE: JavaMain.java

import kotlin.MustUseReturnValue;
import kotlin.IgnorableReturnValue;

@MustUseReturnValue
public class JavaMain {
    public JavaMain() { }

    public String checkedMethod() {
        System.out.println("checkedMethod");
        return("checkedMethod");
    }

    @IgnorableReturnValue
    public String ignoredMethod() {
        System.out.println("ignoredMethod");
        return("ignoredMethod");
    }

    public Exception createException() {
        System.out.println("createException");
        return new RuntimeException("createException");
    }

    public String getProp() {
        return "prop";
    }
}

// FILE: JavaImplicitConstructor.java
import kotlin.MustUseReturnValue;

@MustUseReturnValue
public class JavaImplicitConstructor {}

// FILE: KtFile.kt
fun simple() {
    JavaImplicitConstructor()
    JavaMain()
    JavaMain().prop
    JavaMain().checkedMethod()
    val annotatedClassMember = JavaMain().checkedMethod()
    JavaMain().ignoredMethod()
    val markedToIgnore = JavaMain().ignoredMethod()

    val <!UNDERSCORE_IS_RESERVED!>_<!> = JavaMain().checkedMethod()
}

fun useInAnotherCall() {
    println(JavaMain().checkedMethod())
}

fun getAndThrow() {
    throw JavaMain().createException()
}

fun returnIt(): String {
    return JavaMain().checkedMethod()
}

fun useInCondition() {
    if(JavaMain().checkedMethod() is String) {
        print("It is")
    }
}

fun useInLoop() {
    for(ch in JavaMain().checkedMethod()) {
        print(ch)
    }
}

fun expression() = JavaMain().checkedMethod()

fun main() {
    simple()
    useInAnotherCall()
    getAndThrow()
    returnIt()
    useInCondition()
    useInLoop()
    expression()
}

/* GENERATED_FIR_TAGS: flexibleType, forLoop, functionDeclaration, ifExpression, isExpression, javaFunction, javaType,
localProperty, propertyDeclaration, stringLiteral, unnamedLocalVariable */
