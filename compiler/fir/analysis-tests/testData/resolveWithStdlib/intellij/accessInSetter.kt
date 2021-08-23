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
