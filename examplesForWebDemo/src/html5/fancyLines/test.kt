/*
This example is based on example from html5 canvas2D docs:
    http://www.w3.org/TR/2dcontext/
Note that only a subset of the api is supported for now.
*/

package fancylines

import js.*;
import html5.*;
import jquery.*;

fun main() {
    //jq is a name for JQuery function
    jq {
        FancyLines().run();
    }
}

class FancyLines() {
    // we use two 'magic' functions here getContext() and getCanvas()
    val context = getContext();
    val height = getCanvas().height;
    val width = getCanvas().width;
    var x = width * Math.random();
    var y = height * Math.random();
    var hue = 0;

    fun line() {
        context.save();

        context.beginPath();

        context.lineWidth = 20.0 * Math.random();
        context.moveTo(x, y);

        x = width * Math.random();
        y = height * Math.random();

        context.bezierCurveTo(width * Math.random(), height * Math.random(),
            width * Math.random(), height * Math.random(), x, y);

        hue += Math.random() * 10;

        context.strokeStyle = "hsl($hue, 50%, 50%)";

        context.shadowColor = "white";
        context.shadowBlur = 10.0;

        context.stroke();

        context.restore();
    }

    fun blank() {
        context.fillStyle = "rgba(255,255,1,0.1)";
        context.fillRect(0.0, 0.0, width, height);
    }

    fun run() {
        setInterval({line()}, 40);
        setInterval({blank()}, 100);
    }
}