// TARGET_BACKEND: JVM

// WITH_STDLIB
// FULL_JDK

package testeval

import java.util.LinkedList
import java.util.Deque

interface Expression
class Num(val value : Int) : Expression
class Sum(val left : Expression, val right : Expression) : Expression
class Mult(val left : Expression, val right : Expression) : Expression

fun eval(e : Expression) : Int {
    return when (e) {
        is Num -> e.value
        is Sum -> eval(e.left) + eval (e.right)
        is Mult -> eval(e.left) * eval (e.right)
        else -> throw AssertionError("Unknown expression")
    }
}

interface ParseResult<out T> {
    val success : Boolean
    val value : T
}

class Success<T>(override val value : T) : ParseResult<T> {
    public override val success : Boolean = true
}

class Failure(val message : String) : ParseResult<Nothing> {
    override val success = false
    override val value : Nothing = throw UnsupportedOperationException("Don't call value on a Failure")
}

open class Token(val text : String) {
    override fun toString() = text
}
object LPAR : Token("(")
object RPAR : Token(")")
object PLUS : Token("+")
object TIMES : Token("*")
object EOF : Token("EOF")
class Number(text : String) : Token(text)
class Error(text : String) : Token("[Error: $text]")


fun tokenize(text : String) : Deque<Token> {
    val result = LinkedList<Token>()
    for (c in text) {
        result.add(when (c) {
            '(' -> LPAR
            ')' -> RPAR
            '+' -> PLUS
            '*' -> TIMES
            in '0'..'9' -> Number(c.toString())
            else -> Error(c.toString())
        })
    }
    result.add(EOF)
    return result
}

fun parseSum(tokens : Deque<Token>) : ParseResult<Expression> {
    val left = parseMult(tokens)
    if (!left.success) return left

    if (tokens.peek() == PLUS) {
        tokens.pop()
        val right = parseSum(tokens)
        if (!right.success) return right
        return Success(Sum(left.value, right.value))
    }

    return left
}

fun parseMult(tokens : Deque<Token>) : ParseResult<Expression> {
    val left = parseAtomic(tokens)
    if (!left.success) return left

    if (tokens.peek() == PLUS) {
        tokens.pop()
        val right = parseMult(tokens)
        if (!right.success) return right
        return Success(Mult(left.value, right.value))
    }

    return left
}

fun parseAtomic(tokens : Deque<Token>) : ParseResult<Expression> {
    val token = tokens.poll()
    return when (token) {
        LPAR -> {
            val result = parseSum(tokens)
            val rpar = tokens.poll()
            if (rpar == RPAR)
                result
            else
                Failure("Expecting ')'")
        }
        is Number -> Success(Num(Integer.parseInt((token as Token).text)))
        else -> Failure("Unexpected EOF")
    }
}

fun parse(text : String) : ParseResult<Expression> = parseSum(tokenize(text))

fun box(): String {
    if (1 != eval(Num(1))) return "fail 1"
    if (2 != eval(Sum(Num(1), Num(1)))) return "fail 2"
    if (3 != eval(Mult(Num(3), Num(1)))) return "fail 3"
    if (6 != eval(Mult(Num(3), Sum(Num(1), Num(1))))) return "fail 4"

    if (1 != eval(parse("1").value)) return "fail 5"

    return "OK"
}
