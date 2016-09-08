/**
 * Created by user on 7/21/16.
 */

const main = require("./../main.js");

function handle(httpContent, response) {
    var directionClass = main.protoConstructorControl.DirectionRequest;
    var directionObject = directionClass.decode(httpContent);
    var resultByte = main.thisCar.getDirectionByte(directionObject.command);
    main.thisCar.reset();
    main.thisCar.moveList.push({
        "direction": resultByte,
        "executeTime": 100000
    });
    console.log(resultByte);
    var directionResponse = new main.protoConstructorControl.DirectionResponse({
        "code": 0,
        errorMsg: ""
    });
    var byteBuffer = directionResponse.encode();
    var byteArray = [];
    for (var i = 0; i < byteBuffer.limit; i++) {
        byteArray.push(byteBuffer.buffer[i]);
    }
    response.writeHead(200, {"Content-Type": "text/plain"});
    response.write(new Buffer(byteArray));
    response.end();
    // main.thisCar.setDirection(resultByte, function (code, errorMsg) {
    //     var directionResponse = new main.protoConstructorControl.DirectionResponse({
    //         "code": code,
    //         errorMsg: errorMsg
    //     });
    //     response.writeHead(200, {"Content-Type": "text/plain"});
    //     response.write(directionResponse.encode().buffer);
    //     response.end();
    // });
}

exports.handler = handle;