package demo

import kool.swing.*
import javax.swing.*

val greeting = "Hello,\n\nEnter some text here!"

fun main(args : Array<String>) {
  KFrame("Demo") {
    height = 100
    width = 100
    defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    val text = JTextArea(greeting)
    center = text

    val southPanel = JPanel {
        west = JButton("Clear") {
          text.setText("")
        }

        east = JButton("Restore") {
          text.setText(greeting)
        }
    }

  }.show()

}