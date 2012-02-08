package moving;

import js.*;
import html5.*;
import jquery.*;

fun main() {
    jq {
        Moving().run();
    }
}


class Moving() {

    val context = getContext();
    val height = getCanvas().height;
    val width = getCanvas().width;

    var relX = 0.5;
    var relY = 0.5;

    val absX : Double
        get() = (relX * width);
    val absY : Double
        get() = (relY * height);

    var relXVelocity = randomVelocity();
    var relYVelocity = randomVelocity();


    val message = "Hello Kotlin";
    val textHeightInPixels = 60;
    {
        context.font = "bold ${textHeightInPixels}px Georgia, serif"
    }
    val textWidthInPixels = context.measureText(message).width;

    fun renderText() {
        context.save();
        move();
        context.shadowColor = "white";
        context.shadowBlur = 10.0;
        context.fillStyle = "rgba(100,200,0,0.7)";
        context.fillText(message, absX, absY);
        context.restore();
    }

    fun move() {
        val relTextWidth = textWidthInPixels / width;
        if (relX > (1.0 - relTextWidth - relXVelocity.abs) || relX <  relXVelocity.abs) {
            relXVelocity *= -1;
        }
        val relTextHeight = textHeightInPixels / height;
        if (relY > (1.0 - relYVelocity.abs) || relY < relYVelocity.abs + relTextHeight) {
            relYVelocity *= -1;
        }
        relX += relXVelocity;
        relY += relYVelocity;
    }

    fun changeDirection() {
        relYVelocity = randomVelocity();
        relXVelocity = randomVelocity();
    }


    fun renderBackground() {
        context.save();
        context.fillStyle = "rgba(255,255,1,0.2)";
        context.fillRect(0.0, 0.0, width, height);
        context.restore();
    }

    fun randomVelocity() = 0.01 * Math.random() *
                            if (Math.random() < 0.5) 1 else -1;

    fun run() {
        setInterval({
            renderBackground();
            renderText();
        }, 10);
        setInterval({
            changeDirection();
        }, 3000);
    }

    val Double.abs : Double
    get() = if (this > 0) this else -this

}

