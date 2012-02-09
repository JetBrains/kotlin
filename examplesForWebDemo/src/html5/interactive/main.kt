package interactive;

import jquery.*;
import html5.*;
import java.util.ArrayList;
import js.*;
import js.Math;
import js.setInterval
import html5.getCanvas
import html5.getKotlinLogo
import jquery.jq
import js.setTimeout

val state = CanvasState(getCanvas());

fun doWithPeriod(period : Int, f : ()->Unit) {
    setInterval(f, period);
}

abstract class Shape() {
    abstract fun draw(state : CanvasState);
    abstract fun contains(mousePos : Vector) : Boolean;
    abstract var pos : Vector;
    var selected : Boolean = false;
}

class Circle(override var pos : Vector,
             var radius : Double = 1.0,
             var fillColor : String = "#AAAAAA") : Shape() {

    override fun draw(state : CanvasState) {
        val context = state.context;
        context.shadowColor = "white";
        context.shadowBlur = 10.0;
        context.fillStyle = fillColor;
        context.beginPath();
        context.arc(pos.x, pos.y, radius, 0.0, 2 * Math.PI, false);
        context.closePath();
        context.fill();
    }

    override fun contains(mousePos : Vector) = pos.distanceTo(mousePos) < radius;
}

class Rectangle(override var pos : Vector,
                var size : Vector) : Shape()
{
    override fun draw(state : CanvasState) {
        val context = state.context;
        context.fillStyle = "rgba(0,255,0,.6)";
        context.fillRect(pos.x, pos.y, size.x, size.y);
        if (selected) {
            context.strokeStyle = "#FF0000"
            context.lineWidth = 2.0;
            context.strokeRect(pos.x, pos.y, size.x, size.y)
        }
    }

    override fun contains(mousePos : Vector) = mousePos.isInRect(pos, size);
}

class JB(override var pos : Vector,
         var relSize : Double = 0.3) : Shape()
{

    val imageSize = v(704.0, 254.0)
    val canvasSize = imageSize * relSize

    override fun draw(state : CanvasState) {
        val context = state.context;
        context.drawImage(getKotlinLogo(), 0.0, 0.0, imageSize.x, imageSize.y, pos.x, pos.y, canvasSize.x, canvasSize.y)
    }

    override fun contains(mousePos: Vector): Boolean = mousePos.isInRect(pos, canvasSize)
}

class CanvasState(val canvas : Canvas) {
    val width = canvas.width;
    val height = canvas.height;
    val context = getContext();
    var valid = false;
    var shapes = ArrayList<Shape>();
    var selection : Shape? = null;
    var dragOff = Vector();
    val interval = 1000 / 30;
    val size = 20.0;

    {
        jq(canvas).mousedown {
            valid = false;
            selection = null;
            val mousePos = mousePos(it);
            for (shape in shapes) {
                if (mousePos in shape) {
                    dragOff = mousePos - shape.pos;
                    shape.selected = true;
                    selection = shape;
                    break;
                }
            }
        }

        jq(canvas).mousemove {
            if (selection != null) {
                selection.sure().pos = mousePos(it) - dragOff;
                valid = false;
            }
        }

        jq(canvas).mouseup {
            if (selection != null) {
                selection.sure().selected = false;
            }
            selection = null;
            valid = false;
        }

        jq(canvas).dblclick {
            val redTransparentCircle = Circle(mousePos(it), size, "rgba(200, 100, 100, 0.3)");
            addShape(redTransparentCircle);
            valid = false;
        }

        doWithPeriod(interval) {
            draw();
        }
    }

    fun mousePos(e : MouseEvent) : Vector {
        var offset = Vector();
        var element : DomElement? = canvas;
        while (element != null) {
            val el : DomElement = element.sure();
            offset += Vector(el.offsetLeft, el.offsetTop);
            element = el.offsetParent;
        }
        return Vector(e.pageX, e.pageY) - offset;
    }

    fun addShape(shape : Shape) {
        shapes.add(shape);
        valid = false;
    }

    fun clear() {
        context.fillStyle = "#FFFFFF"
        context.fillRect(0.0, 0.0, width, height)
        context.strokeStyle = "#000000"
        context.lineWidth = 4.0;
        context.strokeRect(0.0, 0.0, width, height)
    }

    fun draw() {
        if (valid) return

        clear();
        for (shape in shapes) {
            shape.draw(this);
        }
        valid = true;
    }
}

fun v(x : Double, y : Double) = Vector(x, y);

class Vector(var x : Double = 0.0, var y : Double = 0.0) {
    fun plus(v : Vector) = v(x + v.x, y + v.y);
    fun minus(v : Vector) = v(x - v.x, y - v.y);
    fun times(coef : Double) = v(x * coef, y * coef);
    fun distanceTo(v : Vector) = Math.sqrt((this - v).sqr);
    fun isInRect(topLeft : Vector, size : Vector) = (x >= topLeft.x) && (x <= topLeft.x + size.x) &&
                                                    (y >= topLeft.y) && (y <= topLeft.y + size.y)
    val sqr : Double
        get() = x * x + y * y;
}

fun main() {
    val state = CanvasState(getCanvas());
    state.addShape(JB(v(1.0, 1.0)));
    setTimeout({
        state.valid = false
    })
}