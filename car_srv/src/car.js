/**
 * Created by user on 7/21/16.
 */

const fs = require("fs");
const main = require("./main.js");
const directionClass = main.protoConstructorControl.DirectionRequest;

function Car() {
    this.uid = "";
    this.x = 0;
    this.y = 0;
    this.angle = 0;
    this.velocityMove = 0.1;
    this.velocityRotation = 10;
    this.paused = false;
    this.transportError = false;
    this.currentDirection = 0;
    this.executeTimeLeft = 0;
    this.moveList = [];//object of direction and time to execute this direction. field: direction:byte, executeTime:double (seconds)
    this.nextMoveListIndex = 0;

    this.reset = reset;
    this.pause = pause;
    this.move = move;
    this.setDirection = setDirection;
    this.resume = resume;
    this.getDirectionByte = getDirectionByte;
    this.setPath = setPath;

    return this;
}
function reset() {
    this.currentDirection = 0;
    this.executeTimeLeft = 0;
    this.moveList = [];
    this.nextMoveListIndex = 0;
}

function pause() {
    this.paused = true;
}

function resume() {
    this.paused = false;
}

function move(delta) {
    var deltaSeconds = delta / 1000;
    console.log("x=" + this.x + " y=" + this.y + " angle=" + this.angle);
    if (this.paused) {
        return;
    }
    if (this.executeTimeLeft > 0) {
        this.executeTimeLeft -= deltaSeconds;
        if (this.currentDirection == this.getDirectionByte(directionClass.Command.left)) {
            this.angle += deltaSeconds * this.velocityRotation;
        } else if (this.currentDirection == this.getDirectionByte(directionClass.Command.right)) {
            this.angle -= deltaSeconds * this.velocityRotation;
        } else if (this.currentDirection == this.getDirectionByte(directionClass.Command.forward)) {
            this.x += this.velocityMove * deltaSeconds * Math.cos(this.angle * Math.PI / 180);
            this.y += this.velocityMove * deltaSeconds * Math.sin(this.angle * Math.PI / 180);
        }
    } else {
        if (this.nextMoveListIndex == this.moveList.length) {
            //this was last move point
            this.setDirection(this.getDirectionByte(directionClass.Command.stop), null)
        } else {
            var currentMove = this.moveList[this.nextMoveListIndex];
            this.currentDirection = currentMove.direction;
            this.executeTimeLeft = currentMove.executeTime;
            this.setDirection(this.currentDirection, null);
            this.nextMoveListIndex++;
        }
    }
}

//args wayPointsList contains object of points. field {distance, angle_delta}. this fun parse this to move list with field direction and move time with using velocity of move and rotation
function setPath(wayPointsList) {
    this.reset();
    for (var i = 0; i < wayPointsList.length; i++) {
        var currentPoint = wayPointsList[i];
        var angle = currentPoint.angle_delta;
        var distance = currentPoint.distance;
        if (angle != 0) {
            //need rotation. check direction left or right
            if (angle >= 180) {
                this.moveList.push({
                    "direction": getDirectionByte(directionClass.Command.right),
                    "executeTime": (angle / this.velocityRotation)
                });
            } else {
                this.moveList.push({
                    "direction": getDirectionByte(directionClass.Command.left),
                    "executeTime": (angle / this.velocityRotation)
                });
            }
        }
        this.moveList.push({
            "direction": getDirectionByte(directionClass.Command.forward),
            "executeTime": (distance / this.velocityMove)
        });
    }
}

function setDirection(directionByte, callBack) {
    fs.appendFile(main.transportFilePath, directionByte, "binary", function (error) {
        var code = 0;
        var errorMsg = "";
        if (error) {
            console.log(error);
            errorMsg = error;
        }
        if (callBack != null) {
            callBack(code, errorMsg)
        }
    });
}

function getDirectionByte(command) {
    resultByte = -1;
    switch (command) {
        case directionClass.Command.stop:
        {
            resultByte = "0";
            break;
        }
        case directionClass.Command.forward:
        {
            resultByte = "1";
            break;
        }
        case directionClass.Command.backward:
        {
            resultByte = "2";
            break;
        }
        case directionClass.Command.right:
        {
            resultByte = "3";
            break;
        }
        case directionClass.Command.left:
        {
            resultByte = "4";
            break;
        }
    }
    return resultByte
}

exports.getCar = Car;