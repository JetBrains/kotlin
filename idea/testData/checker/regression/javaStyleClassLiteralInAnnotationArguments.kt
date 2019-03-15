annotation class A

class B

@A(B.class<error descr="Name expected">)</error><EOLError descr="Expecting ','"></EOLError>
<error descr="[TOO_MANY_ARGUMENTS] Too many arguments for public constructor A() defined in A"><error descr="[TOO_MANY_ARGUMENTS] Too many arguments for public constructor A() defined in A">fun <error descr="[ANONYMOUS_FUNCTION_WITH_NAME] Anonymous functions with names are prohibited">f</error>() {}</error></error><EOLError descr="Expecting a top level declaration"></EOLError><EOLError descr="Expecting ')'"></EOLError>
