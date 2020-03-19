package org.jetbrains.kotlin.tools.projectWizard.wizard.ui.setting

import com.intellij.util.ui.components.BorderLayoutPanel
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.entity.ValidationResult
import org.jetbrains.kotlin.tools.projectWizard.wizard.ui.*
import javax.swing.JComponent
import javax.swing.Spring
import javax.swing.SpringLayout

open class TitledComponentsList(
    private var components: List<TitledComponent>,
    context: Context,
    private val stretchY: Boolean = false,
    private val globalMaxWidth: Int? = null,
    useBigYGap: Boolean = false,
) : DynamicComponent(context) {
    private val ui = BorderLayoutPanel()

    private val yGap = if (useBigYGap) yGapBig else yGapSmall

    init {
        ui.addToCenter(createComponentsPanel(components))
    }

    override val component get() = ui

    override fun navigateTo(error: ValidationResult.ValidationError) {
        components.forEach { it.navigateTo(error) }
    }

    override fun onInit() {
        super.onInit()
        components.forEach { it.onInit() }
    }

    fun setComponents(newComponents: List<TitledComponent>) {
        this.components = newComponents
        ui.removeAll()
        newComponents.forEach(TitledComponent::onInit)
        ui.addToCenter(createComponentsPanel(newComponents))
    }

    private fun createComponentsPanel(components: List<TitledComponent>) = customPanel(SpringLayout()) {
        if (components.isEmpty()) return@customPanel
        val layout = this.layout as SpringLayout

        fun JComponent.constraints() = layout.getConstraints(this)

        val componentsWithLabels = components.mapNotNull { component ->
            if (!component.shouldBeShow()) return@mapNotNull null
            val label = label(component.title?.let { "$it:" }.orEmpty())
            TitledComponentData(
                label.also { add(it) }.constraints(),
                component.component.also { add(it) }.constraints(),
                component.needCentering,
                component.maximumWidth
            )
        }

        fun TitledComponentData.centerConstraint() =
            if (needCentering) component.height * .5f - label.height * .5f
            else 4.asSpring()

        val labelWidth = componentsWithLabels.fold(componentsWithLabels.first().label.width) { spring, row ->
            Spring.max(spring, row.label.width)
        }

        componentsWithLabels.forEach { (label, component, _, componentMaxWidth) ->
            label.width = labelWidth
            val maxWidth = componentMaxWidth ?: globalMaxWidth
            if (maxWidth == null) {
                component[SpringLayout.EAST] = layout.getConstraint(SpringLayout.EAST, this) - xPanelPadding.asSpring()
            } else {
                component.width = maxWidth.asSpring()
            }
        }

        var lastLabel: SpringLayout.Constraints? = null
        var lastComponent: SpringLayout.Constraints? = null

        for (data in componentsWithLabels) {
            val (label, component) = data
            label.x = xPanelPadding.asSpring()
            component.x = label[SpringLayout.EAST] + xGap

            if (lastComponent != null && lastLabel != null) {
                val constraint = lastComponent[SpringLayout.SOUTH] + yGap
                label.y = constraint + data.centerConstraint()
                component.y = constraint
            } else {
                label.y = data.centerConstraint() + yPanelPadding
                component.y = yPanelPadding.asSpring()
            }

            lastLabel = label
            lastComponent = component
        }

        if (stretchY) {
            constraints()[SpringLayout.SOUTH] = lastComponent!![SpringLayout.SOUTH] + yGap
        }
    }

    companion object {
        private const val xGap = 5
        private const val yGapSmall = 6
        private const val yGapBig = 12
        private const val xPanelPadding = 16
        private const val yPanelPadding = 16
    }

    private data class TitledComponentData(
        val label: SpringLayout.Constraints,
        val component: SpringLayout.Constraints,
        val needCentering: Boolean,
        val maximumWidth: Int?,
    )
}

private operator fun Spring.plus(other: Spring) = Spring.sum(this, other)
private operator fun Spring.plus(gap: Int) = Spring.sum(this, Spring.constant(gap))
private operator fun Spring.minus(other: Spring) = this + Spring.minus(other)
private operator fun Spring.unaryMinus() = Spring.minus(this)
private operator fun Spring.times(by: Float) = Spring.scale(this, by)
private fun Int.asSpring() = Spring.constant(this)
private operator fun SpringLayout.Constraints.get(edgeName: String) = getConstraint(edgeName)
private operator fun SpringLayout.Constraints.set(edgeName: String, spring: Spring) {
    setConstraint(edgeName, spring)
}


