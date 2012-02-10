{
    var classes = function () {
        var Vector = Kotlin.Class.create({initialize:function (x, y) {
            this.$x = x;
            this.$y = y;
        }, get_x:function () {
            return this.$x;
        }, get_y:function () {
            return this.$y;
        }, plus:function (v) {
            {
                return interactive2.v_0(this.get_x() + v.get_x(), this.get_y() + v.get_y());
            }
        }, minus:function () {
            {
                return interactive2.v_0(-this.get_x(), -this.get_y());
            }
        }, minus$0:function (v) {
            {
                return interactive2.v_0(this.get_x() - v.get_x(), this.get_y() - v.get_y());
            }
        }, times:function (coef) {
            {
                return interactive2.v_0(this.get_x() * coef, this.get_y() * coef);
            }
        }, distanceTo:function (v) {
            {
                return Math.sqrt(this.minus$0(v).get_sqr());
            }
        }, rotatedBy:function (theta) {
            {
                var sin = Math.sin(theta);
                var cos = Math.cos(theta);
                return interactive2.v_0(this.get_x() * cos - this.get_y() * sin, this.get_x() * sin + this.get_y() * cos);
            }
        }, isInRect:function (topLeft, size) {
            {
                return this.get_x() >= topLeft.get_x() && this.get_x() <= topLeft.get_x() + size.get_x() && this.get_y() >= topLeft.get_y() && this.get_y() <= topLeft.get_y() + size.get_y();
            }
        }, get_sqr:function () {
            {
                return this.get_x() * this.get_x() + this.get_y() * this.get_y();
            }
        }, get_normalized:function () {
            {
                return this.times(1 / Math.sqrt(this.get_sqr()));
            }
        }
        });
        var RadialGradientGenerator = Kotlin.Class.create({initialize:function (context) {
            this.$context = context;
            this.$gradients = new Kotlin.ArrayList;
            this.$current = 0;
            {
                this.newColorStops([
                    [0, '#F59898'],
                    [0.5, '#F57373'],
                    [1, '#DB6B6B']
                ]);
                this.newColorStops([
                    [0.39, 'rgb(140,167,209)'],
                    [0.7, 'rgb(104,139,209)'],
                    [0.85, 'rgb(67,122,217)']
                ]);
                this.newColorStops([
                    [0, 'rgb(255,222,255)'],
                    [0.5, 'rgb(255,185,222)'],
                    [1, 'rgb(230,154,185)']
                ]);
                this.newColorStops([
                    [0, 'rgb(255,209,114)'],
                    [0.5, 'rgb(255,174,81)'],
                    [1, 'rgb(241,145,54)']
                ]);
                this.newColorStops([
                    [0, 'rgb(132,240,135)'],
                    [0.5, 'rgb(91,240,96)'],
                    [1, 'rgb(27,245,41)']
                ]);
                this.newColorStops([
                    [0, 'rgb(250,147,250)'],
                    [0.5, 'rgb(255,80,255)'],
                    [1, 'rgb(250,0,217)']
                ]);
            }
        }, get_context:function () {
            return this.$context;
        }, get_gradients:function () {
            return this.$gradients;
        }, get_current:function () {
            return this.$current;
        }, set_current:function (tmp$0) {
            this.$current = tmp$0;
        }, newColorStops:function (colorStops) {
            {
                this.get_gradients().add(colorStops);
            }
        }, getNext:function () {
            {
                var result = this.get_gradients().get(this.get_current());
                this.set_current((this.get_current() + 1) % this.get_gradients().size());
                return result;
            }
        }
        });
        var CanvasState = Kotlin.Class.create({initialize:function (canvas) {
            this.$canvas = canvas;
            this.$width = this.get_canvas().width;
            this.$height = this.get_canvas().height;
            this.$context = getContext();
            this.$valid = false;
            this.$shapes = new Kotlin.ArrayList;
            this.$selection = null;
            this.$dragOff = new interactive2.Vector_0(0, 0);
            this.$interval = 1000 / 30;
            {
                var tmp$4;
                var tmp$3;
                var tmp$2;
                var tmp$1;
                var tmp$0_0;
                $(this.get_canvas()).mousedown((tmp$0_0 = this , function (it) {
                    {
                        var tmp$0;
                        tmp$0_0.set_valid(false);
                        tmp$0_0.set_selection(null);
                        var mousePos = tmp$0_0.mousePos_0(it);
                        {
                            tmp$0 = tmp$0_0.get_shapes().iterator();
                            while (tmp$0.hasNext()) {
                                var shape = tmp$0.next();
                                {
                                    if (shape.contains(mousePos)) {
                                        tmp$0_0.set_dragOff(mousePos.minus$0(shape.get_pos()));
                                        shape.set_selected(true);
                                        tmp$0_0.set_selection(shape);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                    ));
                $(this.get_canvas()).mousemove((tmp$1 = this , function (it) {
                    {
                        if (tmp$1.get_selection() != null) {
                            Kotlin.sure(tmp$1.get_selection()).set_pos(tmp$1.mousePos_0(it).minus$0(tmp$1.get_dragOff()));
                            tmp$1.set_valid(false);
                        }
                    }
                }
                    ));
                $(this.get_canvas()).mouseup((tmp$2 = this , function (it) {
                    {
                        if (tmp$2.get_selection() != null) {
                            Kotlin.sure(tmp$2.get_selection()).set_selected(false);
                        }
                        tmp$2.set_selection(null);
                        tmp$2.set_valid(false);
                    }
                }
                    ));
                $(this.get_canvas()).dblclick((tmp$3 = this , function (it) {
                    {
                        var redTransparentCircle = new interactive2.Creature_0(tmp$3.mousePos_0(it), tmp$3);
                        tmp$3.addShape(redTransparentCircle);
                        tmp$3.set_valid(false);
                    }
                }
                    ));
                interactive2.doWithPeriod(this.get_interval(), (tmp$4 = this , function () {
                    {
                        tmp$4.draw();
                    }
                }
                    ));
            }
        }, get_canvas:function () {
            return this.$canvas;
        }, get_width:function () {
            return this.$width;
        }, get_height:function () {
            return this.$height;
        }, get_context:function () {
            return this.$context;
        }, get_valid:function () {
            return this.$valid;
        }, set_valid:function (tmp$0) {
            this.$valid = tmp$0;
        }, get_shapes:function () {
            return this.$shapes;
        }, set_shapes:function (tmp$0) {
            this.$shapes = tmp$0;
        }, get_selection:function () {
            return this.$selection;
        }, set_selection:function (tmp$0) {
            this.$selection = tmp$0;
        }, get_dragOff:function () {
            return this.$dragOff;
        }, set_dragOff:function (tmp$0) {
            this.$dragOff = tmp$0;
        }, get_interval:function () {
            return this.$interval;
        }, mousePos_0:function (e) {
            {
                var offset = new interactive2.Vector_0(0, 0);
                var element = this.get_canvas();
                while (element != null) {
                    var el = Kotlin.sure(element);
                    offset = offset.plus(new interactive2.Vector_0(el.offsetLeft, el.offsetTop));
                    element = el.offsetParent;
                }
                return (new interactive2.Vector_0(e.pageX, e.pageY)).minus$0(offset);
            }
        }, addShape:function (shape) {
            {
                this.get_shapes().add(shape);
                this.set_valid(false);
            }
        }, clear:function () {
            {
                this.get_context().fillStyle = '#FFFFFF';
                this.get_context().fillRect(0, 0, this.get_width(), this.get_height());
                this.get_context().strokeStyle = '#000000';
                this.get_context().lineWidth = 4;
                this.get_context().strokeRect(0, 0, this.get_width(), this.get_height());
            }
        }, draw:function () {
            {
                var tmp$0;
                if (this.get_valid())
                    return;
                this.clear();
                {
                    tmp$0 = this.get_shapes().iterator();
                    while (tmp$0.hasNext()) {
                        var shape = tmp$0.next();
                        {
                            shape.draw(this);
                        }
                    }
                }
                this.set_valid(true);
            }
        }
        });
        var Shape = Kotlin.Class.create({initialize:function () {
            this.$selected = false;
        }, draw:function (state) {
        }, contains:function (mousePos) {
        }, get_pos:function () {
            return this.$pos;
        }, set_pos:function (tmp$0) {
            this.$pos = tmp$0;
        }, get_selected:function () {
            return this.$selected;
        }, set_selected:function (tmp$0) {
            this.$selected = tmp$0;
        }
        });
        var Creature = Kotlin.Class.create(Shape, {initialize:function (pos, state) {
            this.$pos = pos;
            this.$state = state;
            this.super_init();
            this.$shadowOffset = interactive2.v_0(-5, 5);
            this.$colorStops = interactive2.get_gradientGenerator().getNext();
            this.$relSize = 0.05;
        }, get_pos:function () {
            return this.$pos;
        }, set_pos:function (tmp$0) {
            this.$pos = tmp$0;
        }, get_state:function () {
            return this.$state;
        }, set_state:function (tmp$0) {
            this.$state = tmp$0;
        }, get_shadowOffset:function () {
            return this.$shadowOffset;
        }, get_colorStops:function () {
            return this.$colorStops;
        }, get_relSize:function () {
            return this.$relSize;
        }, get_radius:function () {
            {
                return this.get_state().get_width() * this.get_relSize();
            }
        }, get_position:function () {
            var tmp$0;
            if (this.get_selected())
                tmp$0 = this.get_pos().minus$0(this.get_shadowOffset());
            else
                tmp$0 = this.get_pos();
            {
                return tmp$0;
            }
        }, get_directionToLogo:function () {
            {
                return interactive2.get_JB().get_centre().minus$0(this.get_position()).get_normalized();
            }
        }, contains:function (mousePos) {
            {
                return this.get_pos().distanceTo(mousePos) < this.get_radius();
            }
        }, fillCircle:function (receiver, position, rad) {
            {
                receiver.beginPath();
                receiver.arc(position.get_x(), position.get_y(), rad, 0, 2 * Math.PI, false);
                receiver.closePath();
                receiver.fill();
            }
        }, draw:function (state) {
            {
                var context = state.get_context();
                if (!this.get_selected()) {
                    this.drawCreature(context);
                }
                else {
                    this.drawCreatureWithShadow(context);
                }
            }
        }, drawCreature:function (context) {
            {
                context.fillStyle = this.getGradient(context);
                this.fillCircle(context, this.get_position(), this.get_radius());
                this.drawEye(context);
                this.drawTail(context);
            }
        }, getGradient:function (context) {
            {
                var tmp$0;
                var gradientCentre = this.get_position().plus(this.get_directionToLogo().times(this.get_radius() / 4));
                var gradient = context.createRadialGradient(gradientCentre.get_x(), gradientCentre.get_y(), 1, gradientCentre.get_x(), gradientCentre.get_y(), 2 * this.get_radius());
                {
                    tmp$0 = Kotlin.arrayIterator(this.get_colorStops());
                    while (tmp$0.hasNext()) {
                        var colorStop = tmp$0.next();
                        {
                            gradient.addColorStop(colorStop[0], colorStop[1]);
                        }
                    }
                }
                return gradient;
            }
        }, drawTail:function (context) {
            {
                var tailDirection = this.get_directionToLogo().minus();
                var tailPos = this.get_position().plus(tailDirection.times(this.get_radius()).times(0.7));
                var tailSize = this.get_radius() * 1.6;
                var angle = Math.PI / 6;
                var p1 = tailPos.plus(tailDirection.rotatedBy(angle).times(tailSize));
                var p2 = tailPos.plus(tailDirection.rotatedBy(-angle).times(tailSize));
                context.fillStyle = this.getGradient(context);
                context.beginPath();
                context.moveTo(tailPos.get_x(), tailPos.get_y());
                context.lineTo(p1.get_x(), p1.get_y());
                var middlePoint = this.get_position().plus(tailDirection.times(this.get_radius()).times(1));
                context.quadraticCurveTo(middlePoint.get_x(), middlePoint.get_y(), p2.get_x(), p2.get_y());
                context.lineTo(tailPos.get_x(), tailPos.get_y());
                context.closePath();
                context.fill();
            }
        }, drawEye:function (context) {
            {
                var eyePos = this.get_directionToLogo().times(this.get_radius()).times(0.6).plus(this.get_position());
                var eyeRadius = this.get_radius() / 3;
                var eyeLidRadius = eyeRadius / 2;
                context.fillStyle = '#FFFFFF';
                this.fillCircle(context, eyePos, eyeRadius);
                context.fillStyle = '#000000';
                this.fillCircle(context, eyePos, eyeLidRadius);
            }
        }, drawCreatureWithShadow:function (context) {
            {
                context.save();
                this.setShadow(context);
                context.fillStyle = this.getGradient(context);
                this.fillCircle(context, this.get_position(), this.get_radius());
                context.restore();
                this.drawEye(context);
                this.drawTail(context);
            }
        }, setShadow:function (context) {
            {
                context.shadowColor = 'rgba(100, 100, 100, 0.7)';
                context.shadowBlur = 5;
                context.shadowOffsetX = this.get_shadowOffset().get_x();
                context.shadowOffsetY = this.get_shadowOffset().get_y();
            }
        }
        });
        var Logo = Kotlin.Class.create(Shape, {initialize:function (pos, relSize) {
            this.$pos = pos;
            this.$relSize = relSize;
            this.super_init();
            this.$imageSize = interactive2.v_0(704, 254);
            this.$canvasSize = this.get_imageSize().times(this.get_relSize());
        }, get_pos:function () {
            return this.$pos;
        }, set_pos:function (tmp$0) {
            this.$pos = tmp$0;
        }, get_relSize:function () {
            return this.$relSize;
        }, set_relSize:function (tmp$0) {
            this.$relSize = tmp$0;
        }, get_imageSize:function () {
            return this.$imageSize;
        }, get_canvasSize:function () {
            return this.$canvasSize;
        }, draw:function (state) {
            {
                var context = state.get_context();
                context.drawImage(getKotlinLogo(), 0, 0, this.get_imageSize().get_x(), this.get_imageSize().get_y(), this.get_pos().get_x(), this.get_pos().get_y(), this.get_canvasSize().get_x(), this.get_canvasSize().get_y());
            }
        }, contains:function (mousePos) {
            {
                return mousePos.isInRect(this.get_pos(), this.get_canvasSize());
            }
        }, get_centre:function () {
            {
                return this.get_pos().plus(this.get_canvasSize().times(0.5));
            }
        }
        });
        return {Shape_0:Shape, Logo_0:Logo, Creature_0:Creature, RadialGradientGenerator_0:RadialGradientGenerator, CanvasState_0:CanvasState, Vector_0:Vector};
    }
        ();
    var interactive2 = Kotlin.Namespace.create({initialize:function () {
        interactive2.$gradientGenerator = new interactive2.RadialGradientGenerator_0(getContext());
        interactive2.$JB = new interactive2.Logo_0(interactive2.v_0(20, 20), 0.3);
    }, get_gradientGenerator:function () {
        return interactive2.$gradientGenerator;
    }, get_JB:function () {
        return interactive2.$JB;
    }, doWithPeriod:function (period, f) {
        {
            setInterval(f, period);
        }
    }, v_0:function (x, y) {
        {
            return new interactive2.Vector_0(x, y);
        }
    }, main:function () {
        {
            var state = new interactive2.CanvasState_0(getCanvas());
            state.addShape(interactive2.get_JB());
            setTimeout(function () {
                    {
                        state.set_valid(false);
                    }
                }
            );
        }
    }
    }, classes);
    interactive2.initialize();
}

var args = [];
interactive2.main(args);
