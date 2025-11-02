# Ideas

## General thoughts

### Is there a limit for amount of branches?
If there is a limit, 2 tests may be written:
1. Green code test, at exactly the limit of branches
2. Red code test, 1 branch above the limit

### Statement is treated as expression with enum as subject
Should it be like that? When is a statement in this case, but requires to be exhaustive

    enum class E {
        A, B, C
    }
    
    fun foo(a: E) {
        when (a) {
            E.A -> print("")
            E.B -> print("")
        }
    }

## Red code

Tests ideas, if not implemented yet, are added here

### If your when expression doesn't have a subject, you must have an else branch

## Green code

Tests ideas, if not implemented yet, are added here

### Should accept boolean functions as branch condition

### Is function a valid subject?

## IDE Integration (hints&improvements)

### Recursion with no exit should be detected

    fun recursion(x: Int = 2): String {
        return when (x) {
            1 -> "One"
            2 -> recursion()
            else -> "Oups"
        }
    }

### Suggestion to merge branches.
Should suggest to merge branches 1 and 2

    enum class E {
        A, B, C
    }
    
    fun foo(a: E): int {
        when (a) {
            E.A -> return 1
            E.B -> return 1
            E.C -> return 0
        }
    }

### Extract return statement. 
Should suggest to get return out of when

    enum class E {
        A, B, C
    }
    
    fun foo(a: E): Int {
        when (a) {
            E.A -> return 1
            E.B -> return 2
            E.C -> return 3
        }
    }

### Ignored return value of expression
Should suggest to process it or convert to statement

### Suggestion to replace with "if" in case one branch condition is always true
This:

    fun foo(e: E): String {
        return when {
            true -> "OK"
            e == E.A -> "Fail"
            else -> "Fail"
        }
    }

Should be replaced with this:

    fun foo(e: E): String {
        return "OK"
    }

If multiple branches conditions always are true, the first one should be left and the rest removed.

### Suggestion to remove branches with always false condition

    fun foo(e: E): String {
        return when {
            e == E.A -> "A"
            false -> "Not A" //this should be suggested for removal 
            else -> "Not A"
        }
    }

### Suggestion to replace series of if - else if - else with when

    enum class E {
        A, B, C
    }
    
    fun foo(e: E) {
        if (e == E.A) {
            print("A")
        } else if (e == E.B) {
            print("B")
        } else {
            print("C")
        }
    }

needs to be detected and replaced with

    enum class E {
        A, B, C
    }

    fun foo(e: E) {
        when (e) {
            E.A -> print("A")
            E.B -> print("B")
            else -> print("C")
        }
    }