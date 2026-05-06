// RUN_PIPELINE_TILL: BACKEND
class DrawableGrid(var isEnabled: Boolean)

class My {
    private val drawableGrid = createDrawableGrid()

    private var useAll = false
        set(value) {
            drawableGrid.isEnabled = !value
        }

    private fun createDrawableGrid() = DrawableGrid(false).apply {
        if (useAll) -1 else 0
    }
}

class Your {
    private val drawableGrid = createDrawableGrid()

    private var useAll
        get() = false
        set(value) {
            drawableGrid.isEnabled = !value
        }

    private fun createDrawableGrid() = DrawableGrid(false).apply {
        if (useAll) -1 else 0
    }
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, getter, ifExpression, integerLiteral,
lambdaLiteral, primaryConstructor, propertyDeclaration, setter */
