namespace animal.farm

import kotlin.io.*
import kotlin.*

fun main(args : Array<String>) {
    val pig = "length: 10";
    val dog = "length: " + pig.length;
    println("Animals are equal: " + pig == dog);

    // Solution:
    println("Animals are equal: " + (pig == dog));

    // Note:
    println("Animals are equal: " + (pig identityEquals dog))
}