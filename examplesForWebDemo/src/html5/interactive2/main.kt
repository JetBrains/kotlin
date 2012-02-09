package interactive2;

import jquery.*;
import html5.*;
import java.util.ArrayList;
import js.*;
import js.Math;
import js.setInterval
import html5.getCanvas
import html5.getJBLogo
import jquery.jq
import js.setTimeout

val gradientGenerator = RadialGradientGenerator(getContext())
val JB = Logo(v(20.0, 20.0))

fun doWithPeriod(period : Int, f : ()->Unit) {
    setInterval(f, period);
}

abstract class Shape() {
    abstract fun draw(state : CanvasState);
    abstract fun contains(mousePos : Vector) : Boolean;
    abstract var pos : Vector;
    var selected : Boolean = false;
}

class Logo(override var pos : Vector,
           var relSize : Double = 0.3)
            : Shape()
{
    val imageSize = v(704.0, 254.0)
    val canvasSize = imageSize * relSize

    override fun draw(state : CanvasState) {
        val context = state.context;
        context.drawImage(getJBLogo(), 0.0, 0.0, imageSize.x, imageSize.y, pos.x, pos.y, canvasSize.x, canvasSize.y)
    }

    override fun contains(mousePos: Vector): Boolean = mousePos.isInRect(pos, canvasSize)

    val centre : Vector
     get() = pos + canvasSize * 0.5
}

class Creature(override var pos : Vector,
               var state : CanvasState) : Shape() {

    val shadowOffset = v(-5.0, 5.0)
    val colorStops = gradientGenerator.getNext()
    val relSize = 0.05;
    val radius : Double
        get() = state.width * relSize;

    val position : Vector
        get() = if (selected) pos - shadowOffset else pos

    val directionToLogo : Vector
        get() = (JB.centre - position).normalized

    override fun contains(mousePos : Vector) = pos.distanceTo(mousePos) < radius;

    fun Context.fillCircle(position : Vector, rad : Double) {
        beginPath();
        arc(position.x, position.y, rad, 0.0, 2 * Math.PI, false);
        closePath();
        fill()
    }

    override fun draw(state : CanvasState) {
        val context = state.context;
        if (!selected) {
            drawCreature(context)
        } else {
            drawCreatureWithShadow(context)
        }
    }

    fun drawCreature(context : Context) {
        context.fillStyle = getGradient(context);
        context.fillCircle(position, radius)
        drawEye(context)
        drawTail(context)
    }

    fun getGradient(context : Context) : CanvasGradient {
        val gradientCentre = position + directionToLogo * (radius / 4)
        val gradient = context.createRadialGradient(gradientCentre.x, gradientCentre.y, 1.0, gradientCentre.x, gradientCentre.y, 2 * radius)
        for (colorStop in colorStops) {
            gradient.addColorStop(colorStop._1, colorStop._2)
        }
        return gradient;
    }

    fun drawTail(context : Context) {
        val tailDirection = -directionToLogo;
        val tailPos = position + tailDirection * radius * 0.7
        val tailSize = radius * 1.6
        val angle = Math.PI / 6.0;
        val p1 = tailPos + tailDirection.rotatedBy(angle) * tailSize
        val p2 = tailPos + tailDirection.rotatedBy(-angle) * tailSize
        context.fillStyle = getGradient(context)
        context.beginPath()
        context.moveTo(tailPos.x, tailPos.y)
        context.lineTo(p1.x, p1.y)
        val middlePoint = position + tailDirection * radius * 1.0
        context.quadraticCurveTo(middlePoint.x, middlePoint.y, p2.x, p2.y)
        context.lineTo(tailPos.x, tailPos.y)
        context.closePath()
        context.fill()
    }

    fun drawEye(context : Context) {;
        val eyePos = directionToLogo * radius * 0.6 + position;
        val eyeRadius = radius / 3;
        val eyeLidRadius = eyeRadius / 2;
        context.fillStyle = "#FFFFFF";
        context.fillCircle(eyePos, eyeRadius)
        context.fillStyle = "#000000"
        context.fillCircle(eyePos, eyeLidRadius)
    }

    fun drawCreatureWithShadow(context : Context) {
        context.save()
        setShadow(context)
        context.fillStyle = getGradient(context)
        context.fillCircle(position, radius)
        context.restore();
        drawEye(context)
        drawTail(context)
    }

    fun setShadow(context : Context) {
        context.shadowColor = "rgba(100, 100, 100, 0.7)";
        context.shadowBlur = 5.0;
        context.shadowOffsetX = shadowOffset.x;
        context.shadowOffsetY = shadowOffset.y;
    }
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
            val redTransparentCircle = Creature(mousePos(it), this @CanvasState);
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

class RadialGradientGenerator(val context : Context) {
    val gradients = ArrayList<Array<#(Double, String)>>();
    var current = 0

    fun newColorStops(vararg colorStops : #(Double, String)) {
        gradients.add(colorStops)
    }

    {
        newColorStops(#(0.0, "#F59898"), #(0.5, "#F57373"), #(1.0, "#DB6B6B"))
        newColorStops(#(0.39, "rgb(140,167,209)"), #(0.7, "rgb(104,139,209)"), #(0.85, "rgb(67,122,217)"))
        newColorStops(#(0.0, "rgb(255,222,255)"), #(0.5, "rgb(255,185,222)"), #(1.0, "rgb(230,154,185)"))
        newColorStops(#(0.0, "rgb(255,209,114)"), #(0.5, "rgb(255,174,81)"), #(1.0, "rgb(241,145,54)"))
        newColorStops(#(0.0, "rgb(132,240,135)"), #(0.5, "rgb(91,240,96)"), #(1.0, "rgb(27,245,41)"))
        newColorStops(#(0.0, "rgb(250,147,250)"), #(0.5, "rgb(255,80,255)"), #(1.0, "rgb(250,0,217)"))
}

    fun getNext() : Array<#(Double, String)> {
        val result = gradients.get(current)
        current = (current + 1) % gradients.size()
        return result
    }
}

fun v(x : Double, y : Double) = Vector(x, y);

class Vector(val x : Double = 0.0, val y : Double = 0.0) {
    fun plus(v : Vector) = v(x + v.x, y + v.y);
    fun minus() = v(-x, -y)
    fun minus(v : Vector) = v(x - v.x, y - v.y)
    fun times(coef : Double) = v(x * coef, y * coef)
    fun distanceTo(v : Vector) = Math.sqrt((this - v).sqr)
    fun rotatedBy(theta : Double) : Vector {
        val sin = Math.sin(theta)
        val cos = Math.cos(theta)
        return v(x * cos - y * sin, x * sin + y * cos)
    }

    fun isInRect(topLeft : Vector, size : Vector) = (x >= topLeft.x) && (x <= topLeft.x + size.x) &&
        (y >= topLeft.y) && (y <= topLeft.y + size.y)

    val sqr : Double
        get() = x * x + y * y
    val normalized : Vector
        get() = this * (1.0 / Math.sqrt(sqr))
}

fun main() {
    val state = CanvasState(getCanvas());
    state.addShape(JB);
    setTimeout({
        state.valid = false
    })
}