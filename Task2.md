Task 2
======

We are going to add an intention action for turning a normal for-loop

    for (x in list) {
        ....
    }

into a for loop with indices

    for ((i, x) in list.withIndices()) {
        ....
    }

Background
----------

[How to create intention actions](http://confluence.jetbrains.com/display/IDEADEV/Creation+of+Intention+Action)
(some extra information: [Language Plugins](http://confluence.jetbrains.com/display/IDEADEV/Developing+Custom+Language+Plugins+for+IntelliJ+IDEA))

Code transformations are done by creating a new PSI node (basically, PSI=Syntax Tree) and replacing the old node by the new one.
To create a new node, it's best to compose some text and run the parser on it. This is done using `JetPsiFactory` class.

Hint: when you need an instance of type `Project`, you can obtain it from any descendant of `JetElement` or `PsiElement`

Simplest version
----------------

Make it work for any for-loop, regardless of the type of the range.
Use this commit as an example:

    d198465 Add an intention to simplify negated binary expressions

(Other examples can be found in the same directory: idea/src/org/jetbrains/jet/plugin/intentions)

Analyze the cases, where the intention should not be applicable, except for the case of `withIndices()` being unavailable.

More Advanced Version
---------------------

Make the intention inapplicable when the range expression's type is not a subtype of `Iterable<Any?>`.

To see how to retrieve a type of an expression, see the `ShowExpressionTypeAction` class (Navigate -> Class... -> <class name>).

To check suptyping use a function called `isSubtypeOf()` (Navigate -> Symbol -> <function name>)

To construct a type symbol for `Iterable<Any?>` use
 - `KotlinBuiltIns`
 - `ClassDescriptor::getDefaultType()`
 - `JetType::getConstructor().getParameters()`
 - `TypeSubstitutor`

Even More Advanced
------------------

- Sometimes the range expression should be parenthesized before you can add `.withIndices()`, e.g.:

```
    for (x in list1 + list2) {
        ....
    }
```
- Check out `_Mapping.kt` to make the applicability condition more precise

Really Advanced
---------------

Allow the user to choose the name of the index variable. Use `ReplaceItWithExplicitFunctionLiteralParamIntention` as an example


Don't forget to add tests
-------------------------