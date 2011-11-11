namespace animal.farm

import std.io.*
import std.*

fun main(args : Array<String>) {
    val pig = "length: 10";
    val dog = "length: " + pig.length;
    println("Animals are equal: " + pig == dog);

    // Solution:
    println("Animals are equal: " + (pig == dog));

    // Note:
    println("Animals are equal: " + (pig identityEquals dog));
}