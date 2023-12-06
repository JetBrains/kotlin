package test

interface MyInterface

class MyClass: MyInterface by Comp<caret>anion {
    companion object : MyInterface
}