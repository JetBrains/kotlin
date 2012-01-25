package kool.swing

import javax.swing.*
import java.awt.*
import java.awt.event.*

var Container.south : JComponent
  get() = throw UnsupportedOperationException()
  set(comp) {add(comp, BorderLayout.SOUTH)}

var Container.north : JComponent
  get() = throw UnsupportedOperationException()
  set(comp) {add(comp, BorderLayout.NORTH)}

var Container.east : JComponent
  get() = throw UnsupportedOperationException()
  set(comp) {add(comp, BorderLayout.EAST)}

var Container.west : JComponent
  get() = throw UnsupportedOperationException()
  set(comp) {add(comp, BorderLayout.WEST)}

var Container.center : JComponent
  get() = throw UnsupportedOperationException()
  set(comp) {add(comp, BorderLayout.CENTER)}

class KFrame(title : String, init : KFrame.() -> Unit) : JFrame(title) {
  {
    this.init()
  }

  fun <T : JComponent> T.toSouth() : T {
    this@KFrame.add(this, BorderLayout.SOUTH)
    return this
  }
}


fun JPanel(init : JPanel.() -> Unit) : JPanel {
  val p = JPanel()
  p.init()
  return p
}

//fun KFrame(title : String, init : JFrame.() -> Unit) : JFrame {
//  val result = JFrame(title)
//  result.init()
//  return result
//}

fun JButton(text : String, action : (ActionEvent) -> Unit) : JButton {
  val result = JButton(text)
  result.addActionListener(object : ActionListener {
      override fun actionPerformed(e: ActionEvent?) {
          action(e.sure())
      }
  })
  return result
}

var JFrame.title : String
  get() = getTitle().sure()
  set(t) {setTitle(t)}

var JFrame.size : #(Int, Int)
  get() = #(getSize().sure().getWidth().int, getSize().sure().getHeight().int)
  set(dim) {setSize(Dimension(dim._1, dim._2))}

var JFrame.height : Int
  get() = getSize().sure().getHeight().int
  set(h) {setSize(width, h)}

var JFrame.width : Int
  get() = getSize().sure().getWidth().int
  set(w) {setSize(height, w)}

var JFrame.defaultCloseOperation : Int
  get() = getDefaultCloseOperation()
  set(def) {setDefaultCloseOperation(def)}

