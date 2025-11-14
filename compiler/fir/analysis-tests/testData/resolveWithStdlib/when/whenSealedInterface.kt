// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

sealed interface ShapeI
class CircleI(val radius: Double) : ShapeI
class RectangleI(val width: Double, val height: Double) : ShapeI

fun area(s: ShapeI): Double =
    when (s) {
        is CircleI -> Math.PI * s.radius * s.radius
        is RectangleI -> s.width * s.height
    }

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, interfaceDeclaration, isExpression, javaProperty,
multiplicativeExpression, primaryConstructor, propertyDeclaration, sealed, smartcast, whenExpression, whenWithSubject */
