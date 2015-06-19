package ppp

import java.security.*

class MyAlgorithmException

fun f1(algorithmException: NoSuchAlgorithmException){}
fun f2(algorithmException: NoSuchAlgorithmException?){}
fun f3(algorithmException: MyAlgorithmException){}

fun f(algorith<caret>)

// EXIST_JAVA_ONLY: { lookupString: "algorithmException", itemText: "algorithmException: NoSuchAlgorithmException", tailText: " (java.security)" }
// EXIST_JAVA_ONLY: { lookupString: "algorithmException", itemText: "algorithmException: NoSuchAlgorithmException?", tailText: " (java.security)" }
// EXIST: { lookupString: "algorithmException", itemText: "algorithmException: MyAlgorithmException", tailText: " (ppp)" }
// NOTHING_ELSE
