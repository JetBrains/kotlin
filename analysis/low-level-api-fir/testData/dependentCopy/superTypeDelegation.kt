package test

interface MyInterface

val property: MyInterface = object : MyInterface {}

class MyClass: MyInterface by prop<caret>erty